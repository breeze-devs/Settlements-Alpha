package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ArmConfiguration;

import javax.annotation.Nonnull;
import java.util.Optional;

public interface IdleLifeAnimator {

    IdleLifeAnimator NONE = new IdleLifeAnimator() {
        @Override
        public void advance(@Nonnull IdleLifeAnimationContext context) {
        }

        @Override
        public AnimationFrame sample(@Nonnull IdleLifeAnimationContext context) {
            return AnimationFrame.EMPTY;
        }

        @Override
        public Optional<ArmConfiguration> activeArmConfiguration(@Nonnull IdleLifeAnimationContext context) {
            return Optional.empty();
        }
    };

    /**
     * Advances blink and fidget lifecycle state without constructing an animation frame. Implementations
     * must tolerate repeated calls for the same context because rendering and arm resolution can both
     * observe the animator during one frame.
     */
    void advance(@Nonnull IdleLifeAnimationContext context);

    /**
     * Samples the already-advanced state without changing timers or selecting animations.
     */
    AnimationFrame sample(@Nonnull IdleLifeAnimationContext context);

    /**
     * Reads the already-advanced discrete arm state.
     */
    Optional<ArmConfiguration> activeArmConfiguration(@Nonnull IdleLifeAnimationContext context);

}
