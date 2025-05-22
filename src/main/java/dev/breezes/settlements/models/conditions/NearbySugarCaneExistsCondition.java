package dev.breezes.settlements.models.conditions;

import dev.breezes.settlements.entities.villager.BaseVillager;
import lombok.Builder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SugarCaneBlock;

public class NearbySugarCaneExistsCondition<E extends BaseVillager> extends NearbyBlockExistsCondition<E, SugarCaneBlock>{
    @Builder
    public NearbySugarCaneExistsCondition(double rangeHorizontal, double rangeVertical) {
        super(rangeHorizontal, rangeVertical, SugarCaneBlock.class, NearbySugarCaneExistsCondition::isValidSugarCane, 1);
    }

    private static boolean isValidSugarCane(BlockPos blockPos, Level level){
        return (level.getBlockState(blockPos.above()).getBlock().equals(Blocks.SUGAR_CANE)) && (level.getBlockState(blockPos.below()).getBlock().equals(Blocks.SUGAR_CANE));
    }
}
