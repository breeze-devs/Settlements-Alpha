package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ArmConfiguration;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class AnimationLayer {

    private KeyframeAnimation currentAnimation;
    private long currentStartGameTime;
    private boolean transientLayer;
    @Nullable
    private KeyframeAnimation outgoingAnimation;
    private long outgoingStartGameTime;

    public static AnimationLayer persistent(@Nonnull KeyframeAnimation animation, long gameTime) {
        return new AnimationLayer(animation, gameTime, false, null, 0L);
    }

    public static AnimationLayer transientAction(@Nonnull KeyframeAnimation animation, long gameTime) {
        return new AnimationLayer(animation, gameTime, true, null, 0L);
    }

    public void replace(@Nonnull KeyframeAnimation animation, long gameTime, boolean transientLayer) {
        this.outgoingAnimation = this.currentAnimation;
        this.outgoingStartGameTime = this.currentStartGameTime;
        this.currentAnimation = animation;
        this.currentStartGameTime = gameTime;
        // Lifetime follows the latest push, not the layer's original kind: a one-shot replacing a
        // sustained loop must still auto-pop, and a sustained loop replacing a one-shot must persist.
        this.transientLayer = transientLayer;
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

    public float weight(long gameTime, float partialTicks) {
        if (!this.transientLayer) {
            return 1.0F;
        }

        float elapsedTicks = this.elapsedTicks(gameTime, partialTicks);
        int durationTicks = this.currentAnimation.getDurationTicks();
        if (elapsedTicks <= durationTicks) {
            return 1.0F;
        }

        int blendOutTicks = this.currentAnimation.getBlendOutTicks();
        if (blendOutTicks <= 0) {
            return 0.0F;
        }
        return 1.0F - Math.clamp((elapsedTicks - durationTicks) / blendOutTicks, 0.0F, 1.0F);
    }

    public boolean isExpired(long gameTime, float partialTicks) {
        if (!this.transientLayer) {
            return false;
        }

        return this.elapsedTicks(gameTime, partialTicks)
                > this.currentAnimation.getDurationTicks() + this.currentAnimation.getBlendOutTicks();
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

}
