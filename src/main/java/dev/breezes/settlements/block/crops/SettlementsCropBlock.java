package dev.breezes.settlements.block.crops;

import lombok.Getter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

@Getter
public abstract class SettlementsCropBlock extends CropBlock {

    private final String cropId;

    private final int growthLightRequirement;

    public SettlementsCropBlock(Properties properties, String cropId, int growthLightRequirement) {
        super(properties);
        this.cropId = cropId;
        this.growthLightRequirement = growthLightRequirement;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(this.getAgeProperty());
    }

    @Override
    public abstract int getMaxAge();

    @Override
    public abstract IntegerProperty getAgeProperty();

    @Override
    protected abstract ItemLike getBaseSeedId();

}
