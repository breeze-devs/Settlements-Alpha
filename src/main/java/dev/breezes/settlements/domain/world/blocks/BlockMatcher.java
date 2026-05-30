package dev.breezes.settlements.domain.world.blocks;

import dev.breezes.settlements.domain.ai.conditions.IBlockCondition;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.util.function.Predicate;

public record BlockMatcher(@Nonnull Predicate<BlockState> statePredicate,
                           @Nonnull IBlockCondition worldPredicate) {

    public boolean matches(@Nonnull BlockState state) {
        return this.statePredicate.test(state);
    }

    public boolean matches(@Nonnull BlockPos pos, @Nonnull BlockState state, @Nonnull Level level) {
        return this.matches(state) && this.worldPredicate.test(pos, level);
    }

    public boolean matches(@Nonnull BlockPos pos, @Nonnull Level level) {
        return this.matches(pos, level.getBlockState(pos), level);
    }

}
