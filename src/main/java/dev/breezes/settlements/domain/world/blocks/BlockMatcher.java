package dev.breezes.settlements.domain.world.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.util.function.Predicate;

public record BlockMatcher(@Nonnull Predicate<BlockState> statePredicate,
                           @Nonnull BlockStateCondition contextPredicate) implements LiveBlockSiteMatcher {

    public BlockMatcher(@Nonnull Predicate<BlockState> statePredicate) {
        this(statePredicate, (ignored, ignored2) -> true);
    }

    public boolean matches(@Nonnull BlockState state) {
        return this.statePredicate.test(state);
    }

    public boolean matches(@Nonnull BlockPos pos, @Nonnull BlockState state, @Nonnull BlockStateView view) {
        return this.matches(state) && this.contextPredicate.test(pos, view);
    }

    public boolean matches(@Nonnull BlockPos pos, @Nonnull BlockState state, @Nonnull Level level) {
        return this.matches(pos, state, new LevelBlockStateView(level));
    }

    @Override
    public boolean matches(@Nonnull BlockPos pos, @Nonnull Level level) {
        return this.matches(pos, level.getBlockState(pos), level);
    }

}
