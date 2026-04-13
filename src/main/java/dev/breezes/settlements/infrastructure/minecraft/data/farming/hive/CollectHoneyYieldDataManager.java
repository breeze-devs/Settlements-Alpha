package dev.breezes.settlements.infrastructure.minecraft.data.farming.hive;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import dev.breezes.settlements.domain.farming.hive.HiveHarvestBlockData;
import dev.breezes.settlements.domain.farming.hive.HiveHarvestExpertisePool;
import dev.breezes.settlements.domain.farming.hive.HiveHarvestItemEntry;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CustomLog
public class CollectHoneyYieldDataManager extends SimpleJsonResourceReloadListener {

    private static final String DIRECTORY_PATH = "settlements/farming/collect_honey";
    private static final String DEFAULT_BLOCK_ID = "default";
    private static final Gson GSON = new GsonBuilder().create();

    private Map<String, HiveHarvestBlockData> blockDataById = Map.of();

    @Inject
    public CollectHoneyYieldDataManager() {
        super(GSON, DIRECTORY_PATH);
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> entries,
                         @Nonnull ResourceManager resourceManager,
                         @Nonnull ProfilerFiller profiler) {
        Map<String, HiveHarvestBlockData> parsed = new LinkedHashMap<>();
        int errorCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            try {
                HiveHarvestBlockData data = GSON.fromJson(entry.getValue(), HiveHarvestBlockData.class);
                HiveHarvestBlockData sanitized = sanitize(data, entry.getKey().toString());
                if (sanitized == null) {
                    errorCount++;
                    continue;
                }
                parsed.put(sanitized.getBlock(), sanitized);
            } catch (Exception e) {
                log.warn("Failed to parse collect honey yield file '{}': {}", entry.getKey(), e.getMessage());
                errorCount++;
            }
        }

        this.blockDataById = Map.copyOf(parsed);
        if (!this.blockDataById.containsKey(DEFAULT_BLOCK_ID)) {
            log.error("Collect honey yields are missing mandatory '{}' fallback file", DEFAULT_BLOCK_ID);
        }
        log.info("Loaded {} collect honey yield files ({} errors)", this.blockDataById.size(), errorCount);
    }

    public void loadForTest(@Nonnull Map<ResourceLocation, JsonElement> entries) {
        this.apply(entries, null, null);
    }

    @Nonnull
    public Map<String, HiveHarvestBlockData> allBlockData() {
        return this.blockDataById;
    }

    @Nonnull
    public List<ItemStack> rollDrops(@Nonnull String expertiseName,
                                     @Nonnull String harvestedBlockId) {
        List<HiveHarvestItemEntry> rolledEntries = this.rollEntries(expertiseName, harvestedBlockId);

        List<ItemStack> drops = new ArrayList<>();
        for (HiveHarvestItemEntry rolledEntry : rolledEntries) {
            ItemStack stack = toItemStack(rolledEntry, harvestedBlockId);
            if (!stack.isEmpty()) {
                drops.add(stack);
            }
        }

        return drops;
    }

    @Nonnull
    List<HiveHarvestItemEntry> rollEntries(@Nonnull String expertiseName,
                                           @Nonnull String harvestedBlockId) {
        HiveHarvestBlockData defaultData = this.blockDataById.get(DEFAULT_BLOCK_ID);
        if (defaultData == null) {
            log.error("Cannot roll collect honey drops without a '{}' fallback", DEFAULT_BLOCK_ID);
            return List.of();
        }

        HiveHarvestBlockData matchedBlockData = this.blockDataById.getOrDefault(harvestedBlockId, defaultData);
        HiveHarvestExpertisePool pool = matchedBlockData.getExpertisePools().get(expertiseName);
        if (pool == null) {
            pool = defaultData.getExpertisePools().get(expertiseName);
        }
        if (pool == null) {
            log.warn("Collect honey yield pool missing for expertise '{}'", expertiseName);
            return List.of();
        }

        List<HiveHarvestItemEntry> drops = new ArrayList<>();
        for (int i = 0; i < pool.getRolls(); i++) {
            selectEntry(pool.getItems()).ifPresent(drops::add);
        }
        return drops;
    }

    private static HiveHarvestBlockData sanitize(HiveHarvestBlockData data, String fileId) {
        if (data == null || data.getBlock() == null || data.getBlock().isBlank()
                || data.getExpertisePools() == null || data.getExpertisePools().isEmpty()) {
            log.warn("Skipping collect honey yield file '{}': missing required structure", fileId);
            return null;
        }

        Map<String, HiveHarvestExpertisePool> pools = new LinkedHashMap<>();
        for (Map.Entry<String, HiveHarvestExpertisePool> entry : data.getExpertisePools().entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                log.warn("Skipping blank expertise pool in '{}'", fileId);
                continue;
            }

            HiveHarvestExpertisePool sanitizedPool = sanitizePool(entry.getValue(), fileId, entry.getKey());
            if (sanitizedPool != null) {
                pools.put(entry.getKey(), sanitizedPool);
            }
        }

        if (pools.isEmpty()) {
            log.warn("Skipping collect honey yield file '{}': no valid expertise pools", fileId);
            return null;
        }

        return HiveHarvestBlockData.builder()
                .block(data.getBlock())
                .expertisePools(Map.copyOf(pools))
                .build();
    }

    private static HiveHarvestExpertisePool sanitizePool(HiveHarvestExpertisePool pool, String fileId, String expertiseName) {
        if (pool == null || pool.getItems() == null || pool.getItems().isEmpty()) {
            log.warn("Skipping collect honey pool '{}' in '{}': missing items", expertiseName, fileId);
            return null;
        }

        List<HiveHarvestItemEntry> items = new ArrayList<>();
        for (HiveHarvestItemEntry item : pool.getItems()) {
            HiveHarvestItemEntry sanitized = sanitizeItem(item, fileId, expertiseName);
            if (sanitized != null) {
                items.add(sanitized);
            }
        }

        if (items.stream().noneMatch(item -> item.getWeight() > 0)) {
            log.warn("Skipping collect honey pool '{}' in '{}': no item with weight > 0", expertiseName, fileId);
            return null;
        }

        return HiveHarvestExpertisePool.builder()
                .rolls(pool.getRolls() <= 0 ? 1 : pool.getRolls())
                .items(List.copyOf(items))
                .build();
    }

    private static HiveHarvestItemEntry sanitizeItem(HiveHarvestItemEntry item, String fileId, String expertiseName) {
        if (item == null || item.getItemId() == null || item.getItemId().isBlank()) {
            log.warn("Skipping collect honey item in '{}' pool '{}' because item id is missing", fileId, expertiseName);
            return null;
        }

        int minCount = Math.max(1, item.getMinCount());
        int maxCount = Math.max(minCount, item.getMaxCount());
        return HiveHarvestItemEntry.builder()
                .itemId(item.getItemId())
                .weight(item.getWeight())
                .minCount(minCount)
                .maxCount(maxCount)
                .build();
    }

    private static Optional<HiveHarvestItemEntry> selectEntry(List<HiveHarvestItemEntry> items) {
        Map<HiveHarvestItemEntry, Double> weights = new LinkedHashMap<>();
        for (HiveHarvestItemEntry item : items) {
            if (item.getWeight() > 0) {
                weights.put(item, item.getWeight());
            }
        }
        if (weights.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(RandomUtil.weightedChoice(weights));
    }

    private static ItemStack toItemStack(HiveHarvestItemEntry itemEntry, String harvestedBlockId) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemEntry.getItemId()));
        if (item == Items.AIR) {
            log.warn("Skipping collect honey drop for block '{}' because item '{}' no longer resolves", harvestedBlockId, itemEntry.getItemId());
            return ItemStack.EMPTY;
        }

        int count = itemEntry.getMinCount() == itemEntry.getMaxCount()
                ? itemEntry.getMinCount()
                : RandomUtil.randomInt(itemEntry.getMinCount(), itemEntry.getMaxCount(), true);
        return new ItemStack(item, count);
    }

}
