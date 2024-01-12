package dev.breezes.settlements.datagen.item;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.registry.BlockRegistry;
import dev.breezes.settlements.registry.ItemRegistry;
import dev.breezes.settlements.util.ResourceLocationUtil;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, SettlementsMod.MOD_ID, helper);
    }

    @Override
    protected void registerModels() {
        simpleItem(ItemRegistry.SAPPHIRE);
        simpleItem(ItemRegistry.RAW_SAPPHIRE);
        simpleItem(ItemRegistry.METAL_DETECTOR);

        simpleBlockItem(BlockRegistry.SAPPHIRE_DOOR);

        fenceItem(BlockRegistry.SAPPHIRE_FENCE, BlockRegistry.SAPPHIRE_BLOCK);
        wallItem(BlockRegistry.SAPPHIRE_WALL, BlockRegistry.SAPPHIRE_BLOCK);
        buttonItem(BlockRegistry.SAPPHIRE_BUTTON, BlockRegistry.SAPPHIRE_BLOCK);

        simpleBlockItem2(BlockRegistry.SAPPHIRE_STAIRS);
        simpleBlockItem2(BlockRegistry.SAPPHIRE_SLAB);
        simpleBlockItem2(BlockRegistry.SAPPHIRE_PRESSURE_PLATE);
        simpleBlockItem2(BlockRegistry.SAPPHIRE_FENCE_GATE);

        trapdoorItem(BlockRegistry.SAPPHIRE_TRAPDOOR);

        simpleItem(ItemRegistry.CORN_SEEDS);
        simpleItem(ItemRegistry.CORN);

        simpleItem(ItemRegistry.BLUEBERRY);
        simpleItem(ItemRegistry.BLUEBERRY_SEEDS);

        simpleBlockItemBlockTexture(BlockRegistry.CAT_MINT);

        withExistingParent(ItemRegistry.RHINO_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(ItemRegistry.BASE_VILLAGER_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
    }

    private ItemModelBuilder simpleItem(RegistryObject<Item> item) {
        return withExistingParent(item.getId().getPath(), new ResourceLocation("item/generated"))
                .texture("layer0", ResourceLocationUtil.mod("item/%s".formatted(item.getId().getPath())));
    }

    private ItemModelBuilder simpleBlockItem(RegistryObject<Block> item) {
        return withExistingParent(item.getId().getPath(), new ResourceLocation("item/generated"))
                .texture("layer0", ResourceLocationUtil.mod("item/%s".formatted(item.getId().getPath())));
    }

    private ItemModelBuilder simpleBlockItem2(RegistryObject<Block> item) {
        return withExistingParent("%s:%s".formatted(SettlementsMod.MOD_ID, ForgeRegistries.BLOCKS.getKey(item.get()).getPath()),
                modLoc(ForgeRegistries.BLOCKS.getKey(item.get()).getPath()));
    }

    private ItemModelBuilder simpleBlockItemBlockTexture(RegistryObject<Block> item) {
        return withExistingParent(item.getId().getPath(), new ResourceLocation("item/generated"))
                .texture("layer0", ResourceLocationUtil.mod("block/%s".formatted(item.getId().getPath())));
    }

    private ItemModelBuilder fenceItem(RegistryObject<Block> block, RegistryObject<Block> baseBlock) {
        return this.withExistingParent(ForgeRegistries.BLOCKS.getKey(block.get()).getPath(), mcLoc("block/fence_inventory"))
                .texture("texture", ResourceLocationUtil.mod("block/%s".formatted(ForgeRegistries.BLOCKS.getKey(baseBlock.get()).getPath())));
    }

    private ItemModelBuilder wallItem(RegistryObject<Block> block, RegistryObject<Block> baseBlock) {
        return this.withExistingParent(ForgeRegistries.BLOCKS.getKey(block.get()).getPath(), mcLoc("block/wall_inventory"))
                .texture("wall", ResourceLocationUtil.mod("block/%s".formatted(ForgeRegistries.BLOCKS.getKey(baseBlock.get()).getPath())));
    }

    private ItemModelBuilder buttonItem(RegistryObject<Block> block, RegistryObject<Block> baseBlock) {
        return this.withExistingParent(ForgeRegistries.BLOCKS.getKey(block.get()).getPath(), mcLoc("block/button_inventory"))
                .texture("button", ResourceLocationUtil.mod("block/%s".formatted(ForgeRegistries.BLOCKS.getKey(baseBlock.get()).getPath())));
    }

    private ItemModelBuilder trapdoorItem(RegistryObject<Block> block) {
        return this.withExistingParent(ForgeRegistries.BLOCKS.getKey(block.get()).getPath(), modLoc("block/%s_bottom".formatted(ForgeRegistries.BLOCKS.getKey(block.get()).getPath())));
    }

}
