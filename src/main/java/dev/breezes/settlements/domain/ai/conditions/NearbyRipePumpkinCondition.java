package dev.breezes.settlements.domain.ai.conditions;

import lombok.Builder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nonnull;

/**
 * Matches pumpkin blocks that have at least one horizontally adjacent attached pumpkin stem.
 * <p>
 * The stem adjacency check distinguishes farmed pumpkins (grown from a stem) from decorative
 * pumpkin blocks placed by players, preventing villagers from destroying builds.
 */
public class NearbyRipePumpkinCondition<E extends Entity> extends NearbyBlockExistsCondition<E> {

    @Builder
    private NearbyRipePumpkinCondition(double rangeHorizontal, double rangeVertical) {
        super(rangeHorizontal, rangeVertical,
                Blocks.PUMPKIN,
                NearbyRipePumpkinCondition::isAttachedToStem,
                1);
    }

    private static boolean isAttachedToStem(@Nonnull BlockPos pos, @Nonnull Level world) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (world.getBlockState(pos.relative(dir)).is(Blocks.ATTACHED_PUMPKIN_STEM)) {
                return true;
            }
        }
        return false;
    }

}
