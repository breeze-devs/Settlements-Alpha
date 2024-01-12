package dev.breezes.settlements.datagen.block;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.block.crops.BlueberryCropBlock;
import dev.breezes.settlements.block.crops.CornCropBlock;
import dev.breezes.settlements.block.crops.SettlementsCropBlock;
import dev.breezes.settlements.block.crops.StrawberryCropBlock;
import dev.breezes.settlements.registry.BlockRegistry;
import dev.breezes.settlements.util.ResourceLocationUtil;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Function;

public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, SettlementsMod.MOD_ID, helper);
    }

    @Override
    protected void registerStatesAndModels() {
        cubeAllWithItem(BlockRegistry.SAPPHIRE_BLOCK);
        cubeAllWithItem(BlockRegistry.RAW_SAPPHIRE_BLOCK);
        cubeAllWithItem(BlockRegistry.SAPPHIRE_ORE);
//        cubeAllWithItem(BlockRegistry.BOUNCE_BLOCK);

        stairsBlock((StairBlock) BlockRegistry.SAPPHIRE_STAIRS.get(), blockTexture(BlockRegistry.SAPPHIRE_BLOCK.get()));
        slabBlock((SlabBlock) BlockRegistry.SAPPHIRE_SLAB.get(), blockTexture(BlockRegistry.SAPPHIRE_BLOCK.get()), blockTexture(BlockRegistry.SAPPHIRE_BLOCK.get()));

        buttonBlock((ButtonBlock) BlockRegistry.SAPPHIRE_BUTTON.get(), blockTexture(BlockRegistry.SAPPHIRE_BLOCK.get()));
        pressurePlateBlock((PressurePlateBlock) BlockRegistry.SAPPHIRE_PRESSURE_PLATE.get(), blockTexture(BlockRegistry.SAPPHIRE_BLOCK.get()));

        fenceBlock((FenceBlock) BlockRegistry.SAPPHIRE_FENCE.get(), blockTexture(BlockRegistry.SAPPHIRE_BLOCK.get()));
        fenceGateBlock((FenceGateBlock) BlockRegistry.SAPPHIRE_FENCE_GATE.get(), blockTexture(BlockRegistry.SAPPHIRE_BLOCK.get()));
        wallBlock((WallBlock) BlockRegistry.SAPPHIRE_WALL.get(), blockTexture(BlockRegistry.SAPPHIRE_BLOCK.get()));

        doorBlockWithRenderType((DoorBlock) BlockRegistry.SAPPHIRE_DOOR.get(), modLoc("block/sapphire_door_bottom"), modLoc("block/sapphire_door_top"), "cutout");
        trapdoorBlockWithRenderType((TrapDoorBlock) BlockRegistry.SAPPHIRE_TRAPDOOR.get(), modLoc("block/sapphire_trapdoor"), true, "cutout");

        makeStrawberryCrop(BlockRegistry.STRAWBERRY_CROP.get(), "strawberry_stage_", "strawberry_stage_");
        makeCornCrop(BlockRegistry.CORN_CROP.get(), "corn_stage_", "corn_stage_");

        // Test blueberry
        makeCrop(BlockRegistry.BLUEBERRY_CROP.get(), BlueberryCropBlock.CROP_ID);

        // Cat mint
        simpleBlockWithItem(BlockRegistry.CAT_MINT.get(), models().cross(blockTexture(BlockRegistry.CAT_MINT.get()).getPath(), blockTexture(BlockRegistry.CAT_MINT.get())).renderType("cutout"));
        simpleBlockWithItem(BlockRegistry.POTTED_CAT_MINT.get(), models().singleTexture("potted_cat_mint", new ResourceLocation("flower_pot_cross"),
                "plant", blockTexture(BlockRegistry.CAT_MINT.get())).renderType("cutout"));
    }

    public void makeStrawberryCrop(CropBlock block, String modelName, String textureName) {
        Function<BlockState, ConfiguredModel[]> function = state -> strawberryStates(state, block, modelName, textureName);
        getVariantBuilder(block).forAllStates(function);
    }

    private ConfiguredModel[] strawberryStates(BlockState state, CropBlock block, String modelName, String textureName) {
        ConfiguredModel model = new ConfiguredModel(
                models()
                        .crop(modelName + state.getValue(((StrawberryCropBlock) block).getAgeProperty()),
                                ResourceLocationUtil.mod("block/%s%s".formatted(textureName, state.getValue(((StrawberryCropBlock) block).getAgeProperty()))))
                        .renderType("cutout")
        );
        return new ConfiguredModel[]{model};
    }

    public void makeCornCrop(CropBlock block, String modelName, String textureName) {
        Function<BlockState, ConfiguredModel[]> function = state -> cornStates(state, block, modelName, textureName);
        getVariantBuilder(block).forAllStates(function);
    }

    private ConfiguredModel[] cornStates(BlockState state, CropBlock block, String modelName, String textureName) {
        ConfiguredModel model = new ConfiguredModel(
                models()
                        .crop(modelName + state.getValue(((CornCropBlock) block).getAgeProperty()),
                                ResourceLocationUtil.mod("block/%s%s".formatted(textureName, state.getValue(((CornCropBlock) block).getAgeProperty()))))
                        .renderType("cutout")
        );
        return new ConfiguredModel[]{model};
    }

    /**
     * Oi, use this
     */
    private void makeCrop(SettlementsCropBlock block, String cropId) {
        this.getVariantBuilder(block).forAllStates(state -> {
            int currentAge = state.getValue(block.getAgeProperty());
            String modelName = "%s_stage_%d".formatted(cropId, currentAge);
            ModelFile cropModel = this.models()
                    .crop(modelName, ResourceLocationUtil.mod("block/%s".formatted(modelName)))
                    .renderType("cutout");
            return new ConfiguredModel[]{new ConfiguredModel(cropModel)};
        });
    }

    private void cubeAllWithItem(RegistryObject<Block> blockRegistryObject) {
        simpleBlockWithItem(blockRegistryObject.get(), cubeAll(blockRegistryObject.get()));
    }

}
