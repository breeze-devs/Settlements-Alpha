package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ArmConfiguration;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Optional;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class DefaultLocomotionAnimator implements LocomotionAnimator {

    private static final float VANILLA_LIMB_SWING_CYCLE = (float) ((Math.PI * 2.0D) / 0.6662D);
    private static final float STANDSTILL_EPSILON = 0.01F;
    private static final float FULL_WEIGHT_LIMB_SWING_AMOUNT = 0.35F;

    private final LocomotionAnimationLibrary library;

    @Override
    public AnimationFrame sample(@Nonnull LocomotionAnimationContext context) {
        float weight = this.weight(context);
        if (weight <= 0.0F) {
            return AnimationFrame.EMPTY;
        }

        KeyframeAnimation animation = this.library.resolve(context.navigationType());
        return AnimationFrame.EMPTY.composeOver(animation.sample(this.animationTick(context, animation)), weight);
    }

    @Override
    public Optional<ArmConfiguration> activeArmConfiguration(@Nonnull LocomotionAnimationContext context) {
        if (this.weight(context) <= 0.0F) {
            return Optional.empty();
        }
        return this.library.resolve(context.navigationType()).armConfigurationAt(0.0F);
    }

    private float animationTick(@Nonnull LocomotionAnimationContext context, @Nonnull KeyframeAnimation animation) {
        if (animation.getDurationTicks() <= 0) {
            return 0.0F;
        }
        // limbSwing is distance-phased rather than time-phased. Mapping one vanilla limb cycle to
        // one authored clip cycle preserves planted-foot cadence across agility-modified speeds.
        return context.limbSwing() * animation.getDurationTicks() / VANILLA_LIMB_SWING_CYCLE;
    }

    @Override
    public float weight(@Nonnull LocomotionAnimationContext context) {
        if (context.limbSwingAmount() <= STANDSTILL_EPSILON) {
            return 0.0F;
        }
        return Math.clamp(context.limbSwingAmount() / FULL_WEIGHT_LIMB_SWING_AMOUNT, 0.0F, 1.0F);
    }

}
