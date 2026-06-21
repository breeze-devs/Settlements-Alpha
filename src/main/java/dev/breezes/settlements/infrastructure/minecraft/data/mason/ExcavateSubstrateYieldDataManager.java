package dev.breezes.settlements.infrastructure.minecraft.data.mason;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import dev.breezes.settlements.domain.mason.ExcavateSubstrateYieldBlockData;
import dev.breezes.settlements.domain.mason.ExcavateSubstrateYieldExpertisePool;
import dev.breezes.settlements.domain.mason.ExcavateSubstrateYieldItemEntry;
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
public class ExcavateSubstrateYieldDataManager extends SimpleJsonResourceReloadListener {

    private static final String DIRECTORY_PATH = "settlements/mason/excavate_substrate";
    private static final String DEFAULT_BLOCK_ID = "default";
    private static final double DEFAULT_SELECTION_WEIGHT = 1.0D;
    private static final Gson GSON = new GsonBuilder().create();

    private Map<String, ExcavateSubstrateYieldBlockData> blockDataById = Map.of();

    @Inject
    public ExcavateSubstrateYieldDataManager() {
        super(GSON, DIRECTORY_PATH);
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> entries,
                         @Nonnull ResourceManager resourceManager,
                         @Nonnull ProfilerFiller profiler) {
        Map<String, ExcavateSubstrateYieldBlockData> parsed = new LinkedHashMap<>();
        int errorCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            try {
                ExcavateSubstrateYieldBlockData data = GSON.fromJson(entry.getValue(), ExcavateSubstrateYieldBlockData.class);
                ExcavateSubstrateYieldBlockData sanitized = sanitize(data, entry.getKey().toString());
                if (sanitized == null) {
                    errorCount++;
                    continue;
                }
                parsed.put(sanitized.getBlock(), sanitized);
            } catch (Exception e) {
                log.warn("Failed to parse mason shovel yield file '{}': {}", entry.getKey(), e.getMessage());
                errorCount++;
            }
        }

        this.blockDataById = Map.copyOf(parsed);
        if (!this.blockDataById.containsKey(DEFAULT_BLOCK_ID)) {
            log.error("Mason shovel yields are missing mandatory '{}' fallback file", DEFAULT_BLOCK_ID);
        }
        log.info("Loaded {} mason shovel yield files ({} errors)", this.blockDataById.size(), errorCount);
    }

    public void loadForTest(@Nonnull Map<ResourceLocation, JsonElement> entries) {
        this.apply(entries, null, null);
    }

    public Map<String, ExcavateSubstrateYieldBlockData> allBlockData() {
        return this.blockDataById;
    }

    public double selectionWeight(@Nonnull String blockId) {
        ExcavateSubstrateYieldBlockData defaultData = this.blockDataById.get(DEFAULT_BLOCK_ID);
        ExcavateSubstrateYieldBlockData matchedData = this.blockDataById.getOrDefault(blockId, defaultData);
        if (matchedData == null) {
            return DEFAULT_SELECTION_WEIGHT;
        }
        return matchedData.getSelectionWeight() > 0 ? matchedData.getSelectionWeight() : DEFAULT_SELECTION_WEIGHT;
    }

    public List<ItemStack> rollDrops(@Nonnull String expertiseName,
                                     @Nonnull String harvestedBlockId) {
        List<ExcavateSubstrateYieldItemEntry> rolledEntries = this.rollEntries(expertiseName, harvestedBlockId);

        List<ItemStack> drops = new ArrayList<>();
        for (ExcavateSubstrateYieldItemEntry rolledEntry : rolledEntries) {
            ItemStack stack = toItemStack(rolledEntry, harvestedBlockId);
            if (!stack.isEmpty()) {
                drops.add(stack);
            }
        }
        return drops;
    }

    public List<ExcavateSubstrateYieldItemEntry> rollEntries(@Nonnull String expertiseName,
                                                             @Nonnull String harvestedBlockId) {
        ExcavateSubstrateYieldBlockData defaultData = this.blockDataById.get(DEFAULT_BLOCK_ID);
        if (defaultData == null) {
            log.error("Cannot roll mason shovel drops without a '{}' fallback", DEFAULT_BLOCK_ID);
            return List.of();
        }

        ExcavateSubstrateYieldBlockData matchedBlockData = this.blockDataById.getOrDefault(harvestedBlockId, defaultData);
        ExcavateSubstrateYieldExpertisePool pool = matchedBlockData.getExpertisePools().get(expertiseName);
        if (pool == null) {
            pool = defaultData.getExpertisePools().get(expertiseName);
        }
        if (pool == null) {
            log.warn("Mason shovel yield pool missing for expertise '{}'", expertiseName);
            return List.of();
        }

        List<ExcavateSubstrateYieldItemEntry> drops = new ArrayList<>();
        for (int i = 0; i < pool.getRolls(); i++) {
            selectEntry(pool.getItems())
                    .ifPresent(drops::add);
        }
        return drops;
    }

    private static ExcavateSubstrateYieldBlockData sanitize(ExcavateSubstrateYieldBlockData data, String fileId) {
        if (data == null || data.getBlock() == null || data.getBlock().isBlank() || data.getExpertisePools() == null || data.getExpertisePools().isEmpty()) {
            log.warn("Skipping mason shovel yield file '{}': missing required structure", fileId);
            return null;
        }

        Map<String, ExcavateSubstrateYieldExpertisePool> pools = new LinkedHashMap<>();
        for (Map.Entry<String, ExcavateSubstrateYieldExpertisePool> entry : data.getExpertisePools().entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                log.warn("Skipping blank expertise pool in '{}'", fileId);
                continue;
            }

            ExcavateSubstrateYieldExpertisePool sanitizedPool = sanitizePool(entry.getValue(), fileId, entry.getKey());
            if (sanitizedPool != null) {
                pools.put(entry.getKey(), sanitizedPool);
            }
        }

        if (pools.isEmpty()) {
            log.warn("Skipping mason shovel yield file '{}': no valid expertise pools", fileId);
            return null;
        }

        double selectionWeight = data.getSelectionWeight() > 0 ? data.getSelectionWeight() : DEFAULT_SELECTION_WEIGHT;
        return ExcavateSubstrateYieldBlockData.builder()
                .block(data.getBlock())
                .selectionWeight(selectionWeight)
                .expertisePools(Map.copyOf(pools))
                .build();
    }

    private static ExcavateSubstrateYieldExpertisePool sanitizePool(ExcavateSubstrateYieldExpertisePool pool, String fileId, String expertiseName) {
        if (pool == null || pool.getItems() == null || pool.getItems().isEmpty()) {
            log.warn("Skipping mason shovel pool '{}' in '{}': missing items", expertiseName, fileId);
            return null;
        }

        List<ExcavateSubstrateYieldItemEntry> items = new ArrayList<>();
        for (ExcavateSubstrateYieldItemEntry item : pool.getItems()) {
            ExcavateSubstrateYieldItemEntry sanitized = sanitizeItem(item, fileId, expertiseName);
            if (sanitized != null) {
                items.add(sanitized);
            }
        }

        if (items.stream().noneMatch(item -> item.getWeight() > 0)) {
            log.warn("Skipping mason shovel pool '{}' in '{}': no item with weight > 0", expertiseName, fileId);
            return null;
        }

        return ExcavateSubstrateYieldExpertisePool.builder()
                .rolls(pool.getRolls() <= 0 ? 1 : pool.getRolls())
                .items(List.copyOf(items))
                .build();
    }

    private static ExcavateSubstrateYieldItemEntry sanitizeItem(ExcavateSubstrateYieldItemEntry item, String fileId, String expertiseName) {
        if (item == null || item.getItemId() == null || item.getItemId().isBlank()) {
            log.warn("Skipping mason shovel item in '{}' pool '{}' because item id is missing", fileId, expertiseName);
            return null;
        }

        int minCount = Math.max(1, item.getMinCount());
        int maxCount = Math.max(minCount, item.getMaxCount());
        return ExcavateSubstrateYieldItemEntry.builder()
                .itemId(item.getItemId())
                .weight(item.getWeight())
                .minCount(minCount)
                .maxCount(maxCount)
                .build();
    }

    private static Optional<ExcavateSubstrateYieldItemEntry> selectEntry(List<ExcavateSubstrateYieldItemEntry> items) {
        Map<ExcavateSubstrateYieldItemEntry, Double> weights = new LinkedHashMap<>();
        for (ExcavateSubstrateYieldItemEntry item : items) {
            if (item.getWeight() > 0) {
                weights.put(item, item.getWeight());
            }
        }
        if (weights.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(RandomUtil.weightedChoice(weights));
    }

    private static ItemStack toItemStack(ExcavateSubstrateYieldItemEntry itemEntry, String harvestedBlockId) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemEntry.getItemId()));
        if (item == Items.AIR) {
            log.warn("Skipping mason shovel drop for block '{}' because item '{}' no longer resolves", harvestedBlockId, itemEntry.getItemId());
            return ItemStack.EMPTY;
        }

        int count = itemEntry.getMinCount() == itemEntry.getMaxCount()
                ? itemEntry.getMinCount()
                : RandomUtil.randomInt(itemEntry.getMinCount(), itemEntry.getMaxCount(), true);
        return new ItemStack(item, count);
    }

}
