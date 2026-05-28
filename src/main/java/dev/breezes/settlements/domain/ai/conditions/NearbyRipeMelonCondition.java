package dev.breezes.settlements.domain.ai.conditions;

import lombok.Builder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nonnull;

/**
 * Matches melon blocks that have at least one horizontally adjacent attached melon stem.
 * <p>
 * Mirrors {@link NearbyRipePumpkinCondition}: stem adjacency separates farmed melons from
 * decorative placements.
 */
public class NearbyRipeMelonCondition<E extends Entity> extends NearbyBlockExistsCondition<E> {

    @Builder
    private NearbyRipeMelonCondition(double rangeHorizontal, double rangeVertical) {
        super(rangeHorizontal, rangeVertical,
                Blocks.MELON,
                NearbyRipeMelonCondition::isAttachedToStem,
                1);
    }

    private static boolean isAttachedToStem(@Nonnull BlockPos pos, @Nonnull Level world) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (world.getBlockState(pos.relative(dir)).is(Blocks.ATTACHED_MELON_STEM)) {
                return true;
            }
        }
        return false;
    }

}
