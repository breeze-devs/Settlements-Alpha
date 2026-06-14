package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.ai.navigation.NavigationType;

import javax.annotation.Nonnull;

public record LocomotionAnimationContext(@Nonnull NavigationType navigationType,
                                         float limbSwing,
                                         float limbSwingAmount) {

    public static LocomotionAnimationContext idle() {
        return new LocomotionAnimationContext(NavigationType.STROLL, 0.0F, 0.0F);
    }

}
