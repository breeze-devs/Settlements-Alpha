package dev.breezes.settlements.domain.ai.conditions;

import lombok.Builder;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;

public class NearbyFullHiveExistsCondition<E extends Entity> extends NearbyBlockExistsCondition<E> {

    @Builder
    private NearbyFullHiveExistsCondition(double rangeHorizontal, double rangeVertical) {
        super(rangeHorizontal, rangeVertical,
                BlockTags.BEEHIVES,
                NearbyFullHiveExistsCondition::isAtCapacity,
                1);
    }

    private static boolean isAtCapacity(BlockPos pos, Level level) {
        return level.getBlockState(pos)
                .getOptionalValue(BeehiveBlock.HONEY_LEVEL)
                .map(honeyLevel -> honeyLevel == BeehiveBlock.MAX_HONEY_LEVELS)
                .orElse(false);
    }

}
