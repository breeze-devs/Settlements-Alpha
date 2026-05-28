package dev.breezes.settlements.domain.ai.conditions;

import lombok.Builder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SweetBerryBushBlock;

/**
 * Matches sweet berry bushes at age 3 (fully ripe, ready to harvest).
 * <p>
 * Age 3 is the maximum growth stage for {@link SweetBerryBushBlock}. Bushes at earlier ages
 * do not yield berries and should be left to grow naturally.
 */
public class NearbyRipeSweetBerryBushCondition<E extends Entity> extends NearbyBlockExistsCondition<E> {

    @Builder
    private NearbyRipeSweetBerryBushCondition(double rangeHorizontal, double rangeVertical) {
        super(rangeHorizontal, rangeVertical,
                state -> state.is(Blocks.SWEET_BERRY_BUSH) && state.getValue(SweetBerryBushBlock.AGE) == SweetBerryBushBlock.MAX_AGE,
                1);
    }

}
