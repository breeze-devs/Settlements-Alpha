package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ArmConfiguration;

import javax.annotation.Nonnull;

public record ArmConfigurationKeyframe(int tick, @Nonnull ArmConfiguration configuration) {

    public ArmConfigurationKeyframe {
        if (tick < 0) {
            throw new IllegalArgumentException("Arm configuration keyframe tick must be non-negative");
        }
    }

}
