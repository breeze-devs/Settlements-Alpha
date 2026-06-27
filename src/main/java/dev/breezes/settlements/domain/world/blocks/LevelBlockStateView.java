package dev.breezes.settlements.domain.world.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

public record LevelBlockStateView(@Nonnull Level level) implements BlockStateView {

    @Override
    public BlockState getBlockState(@Nonnull BlockPos pos) {
        return this.level.getBlockState(pos);
    }

}
