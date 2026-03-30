package dev.breezes.settlements.domain.ai.conditions;

import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.Builder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import static net.minecraft.world.level.block.Blocks.SUGAR_CANE;

public class NearbySugarCaneExistsCondition<E extends BaseVillager> extends NearbyBlockExistsCondition<E>{
    @Builder
    public NearbySugarCaneExistsCondition(double rangeHorizontal, double rangeVertical) {
        super(rangeHorizontal, rangeVertical, SUGAR_CANE, NearbySugarCaneExistsCondition::isValidSugarCane, 1);
    }

    private static boolean isValidSugarCane(BlockPos blockPos, Level level){
        return (level.getBlockState(blockPos.above()).getBlock().equals(SUGAR_CANE)) && (level.getBlockState(blockPos.below()).getBlock().equals(SUGAR_CANE));
    }
}
