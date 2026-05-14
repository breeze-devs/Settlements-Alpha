package dev.breezes.settlements.domain.animation;

import javax.annotation.Nonnull;

public interface Interpolator<V> {

    V interpolate(@Nonnull V from, @Nonnull V to, float t);

}
