package dev.breezes.settlements.datagen;

import dev.breezes.settlements.block.crops.BlueberryCropBlock;
import dev.breezes.settlements.block.crops.CornCropBlock;
import dev.breezes.settlements.block.crops.CropAgeRegistry;
import dev.breezes.settlements.block.crops.StrawberryCropBlock;
import dev.breezes.settlements.registry.BlockRegistry;
import dev.breezes.settlements.registry.ItemRegistry;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collections;
import java.util.List;

public class ModLootTableProvider {

    public static LootTableProvider create(PackOutput output) {
        return new LootTableProvider(output, Collections.emptySet(), List.of(
                new LootTableProvider.SubProviderEntry(ModBlockLootTables::new, LootContextParamSets.BLOCK)
        ));
    }

    public static class ModBlockLootTables extends BlockLootSubProvider {

        protected ModBlockLootTables() {
            super(Collections.emptySet(), FeatureFlags.REGISTRY.allFlags());
        }

        @Override
        protected void generate() {
            this.dropSelf(BlockRegistry.SAPPHIRE_BLOCK.get());
            this.dropSelf(BlockRegistry.RAW_SAPPHIRE_BLOCK.get());

            this.dropSelf(BlockRegistry.SAPPHIRE_STAIRS.get());
            this.dropSelf(BlockRegistry.SAPPHIRE_BUTTON.get());
            this.dropSelf(BlockRegistry.SAPPHIRE_PRESSURE_PLATE.get());
            this.dropSelf(BlockRegistry.SAPPHIRE_TRAPDOOR.get());
            this.dropSelf(BlockRegistry.SAPPHIRE_FENCE.get());
            this.dropSelf(BlockRegistry.SAPPHIRE_FENCE_GATE.get());
            this.dropSelf(BlockRegistry.SAPPHIRE_WALL.get());

            this.add(BlockRegistry.SAPPHIRE_SLAB.get(), (block) -> createSlabItemTable(BlockRegistry.SAPPHIRE_SLAB.get()));
            this.add(BlockRegistry.SAPPHIRE_DOOR.get(), (block) -> createDoorTable(BlockRegistry.SAPPHIRE_DOOR.get()));

            this.add(BlockRegistry.SAPPHIRE_ORE.get(), (block) ->
                    createCustomOreDrops(BlockRegistry.SAPPHIRE_ORE.get(), ItemRegistry.RAW_SAPPHIRE.get(), UniformGenerator.between(1, 3)));

            // Strawberry
            LootItemCondition.Builder strawberryGrownCondition = LootItemBlockStatePropertyCondition
                    .hasBlockStateProperties(BlockRegistry.STRAWBERRY_CROP.get())
                    .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(StrawberryCropBlock.AGE, StrawberryCropBlock.MAX_AGE));
//            this.add(BlockRegistry.STRAWBERRY_CROP.get(), createCropDrops(BlockRegistry.STRAWBERRY_CROP.get(), ItemRegistry.STRAWBERRY.get(), ItemRegistry.STRAWBERRY_SEEDS.get(), strawberryGrownCondition));

            // "Blueberry"
            this.add(BlockRegistry.BLUEBERRY_CROP.get(), CropAgeRegistry.INSTANCE.cropLootTable(BlockRegistry.BLUEBERRY_CROP.get(), BlueberryCropBlock.CROP_ID, ItemRegistry.BLUEBERRY.get(), ItemRegistry.BLUEBERRY_SEEDS.get()));

            // Corn
            LootItemCondition.Builder cornGrownCondition = LootItemBlockStatePropertyCondition
                    .hasBlockStateProperties(BlockRegistry.CORN_CROP.get())
                    .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(CornCropBlock.AGE, CornCropBlock.MAX_AGE)); // TODO: max age is a pitfall if dont override
            this.add(BlockRegistry.CORN_CROP.get(), createCropDrops(BlockRegistry.CORN_CROP.get(), ItemRegistry.CORN.get(), ItemRegistry.CORN_SEEDS.get(), cornGrownCondition));

            // Cat Mint
            this.dropSelf(BlockRegistry.CAT_MINT.get());
            this.add(BlockRegistry.POTTED_CAT_MINT.get(), createPotFlowerItemTable(BlockRegistry.CAT_MINT.get()));
        }

        private LootTable.Builder createCustomOreDrops(Block block, Item dropItem, NumberProvider dropCountProvider) {
            return createSilkTouchDispatchTable(block, this.applyExplosionDecay(block,
                    LootItem.lootTableItem(dropItem)
                            .apply(SetItemCountFunction.setCount(dropCountProvider))
                            .apply(ApplyBonusCount.addUniformBonusCount(Enchantments.BLOCK_FORTUNE))));
        }


        @Override
        protected Iterable<Block> getKnownBlocks() {
            return BlockRegistry.REGISTRY.getEntries().stream().map(RegistryObject::get)::iterator;
        }

    }
}
