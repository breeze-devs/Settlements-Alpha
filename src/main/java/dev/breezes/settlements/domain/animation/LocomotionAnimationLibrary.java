package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.ai.navigation.NavigationType;

import javax.annotation.Nonnull;

public interface LocomotionAnimationLibrary {

    KeyframeAnimation resolve(@Nonnull NavigationType navigationType);

}
