package dev.breezes.settlements.domain.animation;

import javax.annotation.Nonnull;

/**
 * Owns the umbrella's deploy/stow state machine and samples the carry and canopy clips it drives.
 * Lifecycle advancement is separated from frame construction, as in {@link IdleLifeAnimator}.
 */
public interface UmbrellaAnimator {

    UmbrellaAnimator NONE = new UmbrellaAnimator() {
        @Override
        public void advance(@Nonnull UmbrellaAnimationContext context) {
        }

        @Override
        public AnimationFrame sample(@Nonnull UmbrellaAnimationContext context) {
            return AnimationFrame.EMPTY;
        }

        @Override
        public boolean isVisible(@Nonnull UmbrellaAnimationContext context) {
            return false;
        }
    };

    /**
     * Advances the deploy/stow state machine from {@link UmbrellaAnimationContext#shouldDeploy()}.
     * <p>
     * Implementations must tolerate repeated observation of one context: a mid-tick transition must be
     * driven purely by comparing gameTime against stored thresholds, never by an accumulated delta, so a
     * second call for the same tick is a no-op.
     */
    void advance(@Nonnull UmbrellaAnimationContext context);

    /**
     * Samples the already-advanced state without changing it. Carries the carry-clip targets (the arm
     * tilt and the umbrella slot's own lift) and the canopy-clip targets composed into one frame.
     */
    AnimationFrame sample(@Nonnull UmbrellaAnimationContext context);

    /**
     * Whether the attachment should still be rendered.
     */
    boolean isVisible(@Nonnull UmbrellaAnimationContext context);

}
