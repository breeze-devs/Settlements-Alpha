package dev.breezes.settlements.block.crops;

import dev.breezes.settlements.registry.ItemRegistry;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class BlueberryCropBlock extends SettlementsCropBlock {

    public static final String CROP_ID = "blueberry_crop";

    public BlueberryCropBlock() {
        super(
                BlockBehaviour.Properties.copy(Blocks.WHEAT)
                        .noOcclusion()
                        .noCollission(),
                CROP_ID,
                15
        );
    }

    @Override
    public int getMaxAge() {
        return CropAgeRegistry.getMaxAge(CROP_ID);
    }

    @Override
    public IntegerProperty getAgeProperty() {
        return CropAgeRegistry.getAgeProperty(CROP_ID);
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ItemRegistry.BLUEBERRY_SEEDS.get();
    }

}
