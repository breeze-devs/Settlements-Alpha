package dev.breezes.settlements.domain.ai.conditions;

import lombok.Builder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

/**
 * Matches any {@link CropBlock} subclass (wheat, potato, carrot, beetroot, and modded crops) at
 * maximum age — i.e., ready to harvest.
 * <p>
 * The polymorphic {@link CropBlock} check is why this condition uses the
 * {@link NearbyBlockExistsCondition} predicate constructor rather than the block or tag constructors:
 * there is no single block constant or tag that covers all CropBlock subclasses.
 */
public class NearbyRipeCropCondition<E extends Entity> extends NearbyBlockExistsCondition<E> {

    @Builder
    private NearbyRipeCropCondition(double rangeHorizontal, double rangeVertical) {
        super(rangeHorizontal, rangeVertical, NearbyRipeCropCondition::isRipeCropBlock, 1);
    }

    private static boolean isRipeCropBlock(@Nonnull BlockState state) {
        return state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
    }

}
