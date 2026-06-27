package dev.breezes.settlements.domain.world.blocks;

import net.minecraft.core.BlockPos;

import javax.annotation.Nonnull;

@FunctionalInterface
public interface BlockStateCondition {

    boolean test(@Nonnull BlockPos pos, @Nonnull BlockStateView view);

}
