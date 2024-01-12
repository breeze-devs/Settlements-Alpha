package dev.breezes.settlements.block.crops;

import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.Collections;
import java.util.Map;

/**
 * Registry for crop ages
 * - all variables and methods must be static
 */
public final class CropAgeRegistry extends BlockLootSubProvider {

    public static final CropAgeRegistry INSTANCE = new CropAgeRegistry();

    private static final Map<String, Integer> CROP_AGE_REGISTRY = Map.ofEntries(
            Map.entry(BlueberryCropBlock.CROP_ID, 5)
    );

    @Deprecated
    private CropAgeRegistry() {
        super(Collections.emptySet(), FeatureFlagSet.of());
    }

    @Deprecated
    @Override
    protected void generate() {
        // Do nothing, this is a stub
    }

    public static int getMaxAge(String cropId) {
        return CROP_AGE_REGISTRY.get(cropId);
    }

    public static IntegerProperty getAgeProperty(String cropId) {
        return IntegerProperty.create("age", 0, getMaxAge(cropId));
    }

    /**
     * Creates a loot condition that checks if the crop is mature
     *
     * @param cropId    the crop id
     * @param cropBlock the crop block registered in the block registry
     * @return the loot condition
     */
    public static LootItemCondition.Builder cropMatureCondition(String cropId, Block cropBlock) {
        StatePropertiesPredicate.Builder isCropMaturePredicate = StatePropertiesPredicate.Builder.properties()
                .hasProperty(getAgeProperty(cropId), getMaxAge(cropId));
        return LootItemBlockStatePropertyCondition
                .hasBlockStateProperties(cropBlock)
                .setProperties(isCropMaturePredicate);
    }


    public LootTable.Builder cropLootTable(SettlementsCropBlock cropBlock, String cropId, Item cropItem, Item seedItem) {
        return this.createCropDrops(cropBlock, cropItem, seedItem, cropMatureCondition(cropId, cropBlock));
    }

}
