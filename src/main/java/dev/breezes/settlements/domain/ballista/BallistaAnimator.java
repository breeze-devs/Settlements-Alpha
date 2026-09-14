package dev.breezes.settlements.domain.ballista;

import dev.breezes.settlements.domain.animation.AnimationFrame;
import dev.breezes.settlements.domain.animation.AnimationLayer;
import dev.breezes.settlements.domain.animation.BallistaAnimations;
import dev.breezes.settlements.domain.animation.KeyframeAnimation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Poses a ballista's rig from its state machine. The unwound and cocked stages hold still poses, the rest pose and the
 * wind clip's last frame, and winding and firing play their clips between them on one layer.
 * <p>
 * Only updateAnimationFromState changes what the layer plays, so a frame sampled more than once in a tick cannot age
 * the animation.
 * <p>
 * Instants are ticks on the same counter as the states passed in.
 */
public final class BallistaAnimator {

    private static final KeyframeAnimation WIND = BallistaAnimations.wind();
    private static final KeyframeAnimation FIRE = BallistaAnimations.fire();

    private static final AnimationFrame COCKED_POSE = WIND.sample(WIND.getDurationTicks());

    @Nullable
    private AnimationLayer layer;

    @Nullable
    private KeyframeAnimation layerClip;

    private long layerStartedAt;

    /**
     * Starts the clip of an animation, from its own start instant, when the layer is not already playing it.
     */
    public void updateAnimationFromState(@Nonnull BallistaStateMachine state) {
        if (!state.isTransient()) {
            return;
        }

        KeyframeAnimation clip = state.getStage() == BallistaStateMachine.Stage.WINDING ? WIND : FIRE;
        long startedAt = state.getStartedAt();
        if (this.layer != null && clip == this.layerClip && startedAt == this.layerStartedAt) {
            // Same animation occurrence, leave playback unchanged
            return;
        }

        // New animation occurrence, start it or replace the previous one
        if (this.layer == null) {
            this.layer = AnimationLayer.transientAction(clip, startedAt);
        } else {
            this.layer.replace(clip, startedAt, true);
        }

        this.layerClip = clip;
        this.layerStartedAt = startedAt;
    }

    /**
     * The rig's pose at the given instant, as offsets from its rest pose, from whatever the last
     * updateAnimationFromState left playing.
     */
    public AnimationFrame sample(@Nonnull BallistaStateMachine state, long now, float partialTick) {
        // No cross-fade is needed since animations start/end at the shared pose
        if (this.layer != null && !this.layer.isExpired(now, partialTick)) {
            return this.layer.sample(now, partialTick);
        }

        // The clip may have finished before the supplied state was settled
        BallistaStateMachine settled = state.settledAt(now);
        return settled.getStage() == BallistaStateMachine.Stage.COCKED
                ? COCKED_POSE
                : AnimationFrame.EMPTY;
    }

}
