package dev.breezes.settlements.models.conditions;

import dev.breezes.settlements.entities.villager.BaseVillager;
import lombok.Builder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoulSandBlock;
import net.minecraft.world.level.block.state.BlockState;

import static net.minecraft.world.level.block.NetherWartBlock.AGE;

public class NearbySoulSandExistsCondition<E extends BaseVillager> extends NearbyBlockExistsCondition<E, SoulSandBlock>{
    @Builder
    public NearbySoulSandExistsCondition(double rangeHorizontal, double rangeVertical) {
        super(rangeHorizontal, rangeVertical, SoulSandBlock.class, NearbySoulSandExistsCondition::isValidSoulSand, 1);
    }

    private static boolean isValidSoulSand(BlockPos blockPos, Level level){
        BlockState blockStateAbove = level.getBlockState(blockPos.above());
        return blockStateAbove.isAir() || (blockStateAbove.getBlock().equals(Blocks.NETHER_WART) && blockStateAbove.getValue(AGE) == 3);
    }
}
