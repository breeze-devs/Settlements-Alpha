package dev.breezes.settlements.domain.ai.conditions;

import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.Builder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import static net.minecraft.world.level.block.Blocks.NETHER_WART;
import static net.minecraft.world.level.block.Blocks.SOUL_SAND;
import static net.minecraft.world.level.block.NetherWartBlock.AGE;

public class NearbySoulSandExistsCondition<E extends BaseVillager> extends NearbyBlockExistsCondition<E>{
    @Builder
    public NearbySoulSandExistsCondition(double rangeHorizontal, double rangeVertical) {
        super(rangeHorizontal, rangeVertical, SOUL_SAND, NearbySoulSandExistsCondition::isValidSoulSand, 1);
    }

    private static boolean isValidSoulSand(BlockPos blockPos, Level level){
        BlockState blockStateAbove = level.getBlockState(blockPos.above());
        return blockStateAbove.isAir() || (blockStateAbove.is(NETHER_WART) && blockStateAbove.getValue(AGE) == 3);
    }
}
