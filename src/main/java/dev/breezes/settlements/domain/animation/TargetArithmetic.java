package dev.breezes.settlements.domain.animation;

import javax.annotation.Nonnull;

public interface TargetArithmetic<V> {

    V add(@Nonnull V left, @Nonnull V right);

    V subtract(@Nonnull V left, @Nonnull V right);

    V scale(@Nonnull V value, float scalar);

    V multiply(@Nonnull V left, @Nonnull V right);

}
