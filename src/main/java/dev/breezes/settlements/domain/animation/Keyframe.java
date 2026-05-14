package dev.breezes.settlements.domain.animation;

import javax.annotation.Nonnull;

public record Keyframe<V>(int tick, @Nonnull V value, @Nonnull Easing easingToNext) {

    public Keyframe {
        if (tick < 0) {
            throw new IllegalArgumentException("Keyframe tick must be non-negative");
        }
    }

}
