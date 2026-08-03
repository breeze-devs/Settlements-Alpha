package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ArmConfiguration;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class AnimationLayer {

    private static final long NOT_CLEARING = Long.MIN_VALUE;

    private KeyframeAnimation currentAnimation;
    private long currentStartGameTime;
    private boolean transientLayer;
    @Nullable
    private KeyframeAnimation outgoingAnimation;
    private long outgoingStartGameTime;
    private long clearStartGameTime;
    private float clearWeightFloor;

    /**
     * Weight this layer already held when its current animation was pushed. Rising from here rather than
     * from zero keeps the layer continuous when a push lands while it was already fading.
     */
    private float entryWeightFloor;

    public static AnimationLayer persistent(@Nonnull KeyframeAnimation animation, long gameTime) {
        return new AnimationLayer(animation, gameTime, false, null, 0L, NOT_CLEARING, 0.0F, 0.0F);
    }

    public static AnimationLayer transientAction(@Nonnull KeyframeAnimation animation, long gameTime) {
        return new AnimationLayer(animation, gameTime, true, null, 0L, NOT_CLEARING, 0.0F, 0.0F);
    }

    public void replace(@Nonnull KeyframeAnimation animation, long gameTime, boolean transientLayer) {
        this.entryWeightFloor = this.weight(gameTime, 0.0F);
        this.outgoingAnimation = this.currentAnimation;
        this.outgoingStartGameTime = this.currentStartGameTime;
        this.currentAnimation = animation;
        this.currentStartGameTime = gameTime;
        // Lifetime follows the latest push, not the layer's original kind: a one-shot replacing a
        // sustained loop must still auto-pop, and a sustained loop replacing a one-shot must persist.
        this.transientLayer = transientLayer;
        this.clearStartGameTime = NOT_CLEARING;
        this.clearWeightFloor = 0.0F;
    }

    /**
     * Starts this layer's blend-out. It keeps sampling until the blend finishes, so it fades out of the
     * composite rather than vanishing from it.
     */
    public void beginClear(long gameTime) {
        if (this.clearStartGameTime != NOT_CLEARING) {
            // Re-arming would restart the fade from the top and undo the part already played out.
            return;
        }
        this.clearWeightFloor = this.weight(gameTime, 0.0F);
        this.clearStartGameTime = gameTime;
    }

    public AnimationFrame sample(long gameTime, float partialTicks) {
        AnimationFrame currentFrame = this.sample(this.currentAnimation, this.currentStartGameTime, gameTime, partialTicks);
        if (this.outgoingAnimation == null) {
            return currentFrame;
        }

        float blendProgress = this.blendProgress(gameTime, partialTicks);
        AnimationFrame outgoingFrame = this.sample(this.outgoingAnimation, this.outgoingStartGameTime, gameTime, partialTicks);
        AnimationFrame blendedFrame = outgoingFrame.blendTo(currentFrame, blendProgress);
        if (blendProgress >= 1.0F) {
            this.outgoingAnimation = null;
        }
        return blendedFrame;
    }

    /**
     * How strongly this layer contributes to the composited frame.
     */
    public float weight(long gameTime, float partialTicks) {
        float weight = this.entryWeightFloor
                + (1.0F - this.entryWeightFloor) * this.blendProgress(gameTime, partialTicks);

        float elapsedTicks = this.elapsedTicks(gameTime, partialTicks);
        if (this.transientLayer && elapsedTicks > this.currentAnimation.getDurationTicks()) {
            weight = Math.min(weight, this.blendOutWeight(elapsedTicks - this.currentAnimation.getDurationTicks()));
        }
        if (this.clearStartGameTime != NOT_CLEARING) {
            weight = Math.min(weight, this.clearWeightFloor
                    * this.blendOutWeight(this.elapsedSinceClear(gameTime, partialTicks)));
        }
        return weight;
    }

    public boolean isExpired(long gameTime, float partialTicks) {
        if (this.clearStartGameTime != NOT_CLEARING
                && this.elapsedSinceClear(gameTime, partialTicks) >= this.currentAnimation.getBlendOutTicks()) {
            return true;
        }
        if (!this.transientLayer) {
            return false;
        }

        // With a blend-out the layer reaches zero weight exactly at duration + blendOut, so that tick is
        // already spent; without one it holds full weight through the duration and drops only past it.
        int blendOutTicks = this.currentAnimation.getBlendOutTicks();
        float expiryTick = this.currentAnimation.getDurationTicks() + blendOutTicks;
        float elapsedTicks = this.elapsedTicks(gameTime, partialTicks);
        return blendOutTicks <= 0
                ? elapsedTicks > expiryTick
                : elapsedTicks >= expiryTick;
    }

    public Optional<ArmConfiguration> activeArmConfiguration(long gameTime, float partialTicks) {
        return this.currentAnimation.armConfigurationAt(this.elapsedTicks(gameTime, partialTicks));
    }

    private AnimationFrame sample(@Nonnull KeyframeAnimation animation,
                                  long startGameTime,
                                  long gameTime,
                                  float partialTicks) {
        return animation.sample(Math.max(0.0F, (gameTime - startGameTime) + partialTicks));
    }

    private float blendOutWeight(float elapsedBlendOutTicks) {
        int blendOutTicks = this.currentAnimation.getBlendOutTicks();
        if (blendOutTicks <= 0) {
            return 0.0F;
        }
        return 1.0F - Math.clamp(elapsedBlendOutTicks / blendOutTicks, 0.0F, 1.0F);
    }

    /**
     * How far the current animation is through its blend-in. Doubles as the crossfade position
     * against the outgoing animation and as this layer's own rise toward full weight.
     */
    private float blendProgress(long gameTime, float partialTicks) {
        int blendInTicks = this.currentAnimation.getBlendInTicks();
        if (blendInTicks <= 0) {
            return 1.0F;
        }

        return Math.clamp(this.elapsedTicks(gameTime, partialTicks) / blendInTicks, 0.0F, 1.0F);
    }

    private float elapsedTicks(long gameTime, float partialTicks) {
        return Math.max(0.0F, (gameTime - this.currentStartGameTime) + partialTicks);
    }

    private float elapsedSinceClear(long gameTime, float partialTicks) {
        return Math.max(0.0F, (gameTime - this.clearStartGameTime) + partialTicks);
    }

}
