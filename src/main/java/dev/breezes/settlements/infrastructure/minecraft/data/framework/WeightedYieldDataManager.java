package dev.breezes.settlements.infrastructure.minecraft.data.framework;

import dev.breezes.settlements.domain.common.yields.WeightedYieldItem;
import dev.breezes.settlements.domain.common.yields.WeightedYieldPool;
import dev.breezes.settlements.domain.common.yields.WeightedYieldTable;
import dev.breezes.settlements.domain.common.yields.WeightedYieldTableCodec;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Shared base for datapack-backed weighted block-drop tables.
 * Every file decodes to one {@link WeightedYieldTable}, keyed by its block id,
 * with a mandatory {@code default} fallback used whenever a harvested block or expertise has no
 * dedicated entry.
 */
@CustomLog
public abstract class WeightedYieldDataManager extends CodecJsonDataManager<WeightedYieldTable> {

    protected static final String DEFAULT_BLOCK_ID = "default";
    protected static final double DEFAULT_SELECTION_WEIGHT = 1.0D;

    private final String label;
    private Map<String, WeightedYieldTable> byBlock = Map.of();

    protected WeightedYieldDataManager(@Nonnull String directory, @Nonnull String label) {
        super(directory, WeightedYieldTableCodec.CODEC);
        this.label = label;
    }

    @Override
    protected final String label() {
        return this.label;
    }

    @Override
    protected void onReloaded(@Nonnull Map<ResourceLocation, WeightedYieldTable> values) {
        Map<String, WeightedYieldTable> merged = new LinkedHashMap<>();
        for (WeightedYieldTable value : values.values()) {
            merged.put(value.block(), value);
        }

        this.byBlock = Map.copyOf(merged);
        if (!this.byBlock.containsKey(DEFAULT_BLOCK_ID)) {
            log.error("{} data is missing mandatory '{}' fallback file", this.label, DEFAULT_BLOCK_ID);
        }
    }

    public Map<String, WeightedYieldTable> allBlockData() {
        return this.byBlock;
    }

    public List<ItemStack> rollDrops(@Nonnull String expertiseName,
                                     @Nonnull String harvestedBlockId) {
        List<WeightedYieldItem> rolledEntries = this.rollEntries(expertiseName, harvestedBlockId);

        List<ItemStack> drops = new ArrayList<>();
        for (WeightedYieldItem rolledEntry : rolledEntries) {
            toItemStack(rolledEntry, harvestedBlockId, this.label)
                    .ifPresent(drops::add);
        }

        return drops;
    }

    public List<WeightedYieldItem> rollEntries(@Nonnull String expertiseName,
                                               @Nonnull String harvestedBlockId) {
        WeightedYieldTable defaultData = this.byBlock.get(DEFAULT_BLOCK_ID);
        if (defaultData == null) {
            log.error("Cannot roll {} drops without a '{}' fallback", this.label, DEFAULT_BLOCK_ID);
            return List.of();
        }

        WeightedYieldTable matchedData = this.byBlock.getOrDefault(harvestedBlockId, defaultData);
        WeightedYieldPool pool = matchedData.pools().get(expertiseName);
        if (pool == null) {
            pool = defaultData.pools().get(expertiseName);
        }
        if (pool == null) {
            log.warn("{} pool missing for expertise '{}'", this.label, expertiseName);
            return List.of();
        }

        List<WeightedYieldItem> drops = new ArrayList<>();
        for (int i = 0; i < pool.rolls(); i++) {
            RandomUtil.weightedChoice(pool.items(), WeightedYieldItem::weight).ifPresent(drops::add);
        }
        return drops;
    }

    public double selectionWeight(@Nonnull String blockId) {
        WeightedYieldTable defaultData = this.byBlock.get(DEFAULT_BLOCK_ID);
        WeightedYieldTable matchedData = this.byBlock.getOrDefault(blockId, defaultData);
        if (matchedData == null) {
            return DEFAULT_SELECTION_WEIGHT;
        }

        return matchedData.selectionWeight();
    }

    private Optional<ItemStack> toItemStack(@Nonnull WeightedYieldItem itemEntry, @Nonnull String harvestedBlockId, @Nonnull String label) {
        Item item = BuiltInRegistries.ITEM.get(itemEntry.item());
        if (item == Items.AIR) {
            log.warn("Skipping {} drop for block '{}' because item '{}' no longer resolves", label, harvestedBlockId, itemEntry.item());
            return Optional.empty();
        }

        int count = itemEntry.minCount() == itemEntry.maxCount()
                ? itemEntry.minCount()
                : RandomUtil.randomInt(itemEntry.minCount(), itemEntry.maxCount(), true);
        return Optional.of(new ItemStack(item, count));
    }

}
