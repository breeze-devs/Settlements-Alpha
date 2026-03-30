package dev.breezes.settlements.domain.ai.conditions;

import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.Builder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;

public class NearbyOreExistsCondition<E extends BaseVillager> extends NearbyBlockExistsCondition<E>{
    @Builder
    public NearbyOreExistsCondition(double rangeHorizontal, double rangeVertical) {
        super(rangeHorizontal, rangeVertical, Tags.Blocks.ORES, NearbyOreExistsCondition::isValidOre, 1);
    }

    private static boolean isValidOre(BlockPos blockPos, Level level){
        return level.getBlockState(blockPos.above()).isAir() || level.getBlockState(blockPos.below()).isAir() || level.getBlockState(blockPos.north()).isAir() || level.getBlockState(blockPos.south()).isAir() || level.getBlockState(blockPos.east()).isAir() || level.getBlockState(blockPos.west()).isAir();
    }
}
