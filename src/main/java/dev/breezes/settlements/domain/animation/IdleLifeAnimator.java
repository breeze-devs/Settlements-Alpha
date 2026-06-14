package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ArmConfiguration;

import javax.annotation.Nonnull;
import java.util.Optional;

public interface IdleLifeAnimator {

    IdleLifeAnimator NONE = new IdleLifeAnimator() {
        @Override
        public AnimationFrame sample(@Nonnull IdleLifeAnimationContext context) {
            return AnimationFrame.EMPTY;
        }

        @Override
        public Optional<ArmConfiguration> activeArmConfiguration(@Nonnull IdleLifeAnimationContext context) {
            return Optional.empty();
        }
    };

    AnimationFrame sample(@Nonnull IdleLifeAnimationContext context);

    Optional<ArmConfiguration> activeArmConfiguration(@Nonnull IdleLifeAnimationContext context);

}
