package dev.breezes.settlements.domain.world.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

/**
 * State-only view of blocks around a candidate position
 * <p>
 * Keeping matchers behind this narrow read model prevents resource discovery logic from
 * depending on live-world APIs that cannot safely cross into immutable snapshot scans later
 */
@FunctionalInterface
public interface BlockStateView {

    BlockState getBlockState(@Nonnull BlockPos pos);

}
