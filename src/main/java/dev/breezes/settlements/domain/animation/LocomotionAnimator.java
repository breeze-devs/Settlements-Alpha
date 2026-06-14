package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ArmConfiguration;

import javax.annotation.Nonnull;
import java.util.Optional;

public interface LocomotionAnimator {

    LocomotionAnimator NONE = new LocomotionAnimator() {
        @Override
        public AnimationFrame sample(@Nonnull LocomotionAnimationContext context) {
            return AnimationFrame.EMPTY;
        }

        @Override
        public float weight(@Nonnull LocomotionAnimationContext context) {
            return 0.0F;
        }

        @Override
        public Optional<ArmConfiguration> activeArmConfiguration(@Nonnull LocomotionAnimationContext context) {
            return Optional.empty();
        }
    };

    AnimationFrame sample(@Nonnull LocomotionAnimationContext context);

    /**
     * Fraction this layer contributes this frame, ramping 0 (standstill) to 1 (full gait). Callers
     * fade additive ambience such as idle-life by {@code 1 - weight} so it recedes as the villager moves.
     */
    float weight(@Nonnull LocomotionAnimationContext context);

    Optional<ArmConfiguration> activeArmConfiguration(@Nonnull LocomotionAnimationContext context);

}
