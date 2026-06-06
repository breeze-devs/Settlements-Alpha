package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ArmConfiguration;
import lombok.CustomLog;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@CustomLog
public final class VillagerAnimator {

    private final AnimationResolver animationResolver;
    @Getter
    private AnimationArchetype lastSeenArchetype;
    @Getter
    private byte lastSeenGeneration;
    private AnimationSelectionContext lastResolvedContext;
    private KeyframeAnimation currentAnimation;
    private long currentStartGameTime;
    @Nullable
    private KeyframeAnimation outgoingAnimation;
    private long outgoingStartGameTime;

    public VillagerAnimator(@Nonnull AnimationResolver animationResolver) {
        this.animationResolver = animationResolver;
        this.lastSeenArchetype = AnimationArchetype.IDLE;
        this.lastSeenGeneration = 0;
        this.lastResolvedContext = AnimationSelectionContext.generic();
        this.currentAnimation = animationResolver.resolve(AnimationArchetype.IDLE, AnimationSelectionContext.generic());
        this.currentStartGameTime = 0L;
    }

    public void onMotionChanged(@Nonnull AnimationArchetype newArchetype,
                                byte newGeneration,
                                @Nonnull AnimationSelectionContext context,
                                long gameTime) {
        if (newArchetype == this.lastSeenArchetype && newGeneration == this.lastSeenGeneration) {
            return;
        }

        this.outgoingAnimation = this.currentAnimation;
        this.outgoingStartGameTime = this.currentStartGameTime;
        this.currentAnimation = this.animationResolver.resolve(newArchetype, context);
        this.currentStartGameTime = gameTime;
        this.lastSeenArchetype = newArchetype;
        this.lastSeenGeneration = newGeneration;
        this.lastResolvedContext = context;

        log.debug("VillagerAnimator: motion change {} -> {} (gen={}, context: {}), resolved '{}' [blend in: {} ticks, loop: {}]",
                this.outgoingAnimation.getId(), newArchetype, newGeneration,
                context.mainHandCategory(),
                this.currentAnimation.getId(),
                this.currentAnimation.getBlendInTicks(),
                this.currentAnimation.getLoopMode());
    }

    /**
     * Re-resolves the current animation when the item category catches up after archetype sync.
     * The archetype sync (SynchedEntityData) arrives before the equipment sync
     * (ClientboundSetEquipmentPacket), so the first resolution may use GENERIC. This method
     * corrects that on the next frame without treating it as an archetype change.
     */
    public void tickContext(@Nonnull AnimationSelectionContext context, long gameTime) {
        if (context.equals(this.lastResolvedContext)) {
            return;
        }
        this.lastResolvedContext = context;

        KeyframeAnimation reresolved = this.animationResolver.resolve(this.lastSeenArchetype, context);
        if (reresolved.getId().equals(this.currentAnimation.getId())) {
            return;
        }

        log.debug("VillagerAnimator: context update for archetype {} ({} -> {}), switching '{}' -> '{}'",
                this.lastSeenArchetype,
                this.lastResolvedContext.mainHandCategory(), context.mainHandCategory(),
                this.currentAnimation.getId(), reresolved.getId());
        this.outgoingAnimation = this.currentAnimation;
        this.outgoingStartGameTime = this.currentStartGameTime;
        this.currentAnimation = reresolved;
        this.currentStartGameTime = gameTime;
    }

    /**
     * Arm config is a discrete render state that snaps to the incoming animation at transition
     * rather than blending — geometry visibility cannot be crossfaded, so we always read the
     * current (not outgoing) animation's declared config.
     */
    public ArmConfiguration currentArmConfiguration() {
        return this.currentAnimation.getArmConfiguration();
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
            // Dropping the outgoing animation after sampling prevents one-frame pops at the end of the blend.
            this.outgoingAnimation = null;
        }
        return blendedFrame;
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

        float elapsedTicks = Math.max(0.0F, (gameTime - this.currentStartGameTime) + partialTicks);
        return Math.clamp(elapsedTicks / blendInTicks, 0.0F, 1.0F);
    }

}
