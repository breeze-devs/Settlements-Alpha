package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ArmConfiguration;
import lombok.CustomLog;
import lombok.Getter;

import javax.annotation.Nonnull;

@CustomLog
public final class VillagerAnimator {

    private final AnimationResolver animationResolver;
    @Getter
    private AnimationArchetype lastSeenArchetype;
    @Getter
    private byte lastSeenGeneration;
    private AnimationSelectionContext lastResolvedContext;
    private final LayerStack layerStack;

    // Sleep is a vanilla-synced state read off the client, not a motion-plane archetype. While it is
    // active the animator samples the sleep clip directly and bypasses the whole base/locomotion/
    // idle-life/action fold, so a sleeping villager neither breathes idly, blinks, nor fidgets.
    private final KeyframeAnimation sleepAnimation;
    private boolean sleeping;
    private long sleepStartGameTime;

    public VillagerAnimator(@Nonnull AnimationResolver animationResolver) {
        this(animationResolver, IdleLifeAnimator.NONE, LocomotionAnimator.NONE, 0);
    }

    public VillagerAnimator(@Nonnull AnimationResolver animationResolver,
                            @Nonnull IdleLifeAnimator idleLifeAnimator,
                            @Nonnull LocomotionAnimator locomotionAnimator,
                            int entityId) {
        this.animationResolver = animationResolver;
        this.lastSeenArchetype = AnimationArchetype.IDLE;
        this.lastSeenGeneration = 0;
        this.lastResolvedContext = AnimationSelectionContext.generic();
        this.layerStack = new LayerStack(
                animationResolver.resolve(AnimationArchetype.IDLE, AnimationSelectionContext.generic()),
                idleLifeAnimator,
                locomotionAnimator,
                entityId);
        this.sleepAnimation = animationResolver.resolve(AnimationArchetype.SLEEP, AnimationSelectionContext.generic());
        this.sleeping = false;
        this.sleepStartGameTime = 0L;
    }

    /**
     * Polls the villager's vanilla sleep state each frame. Entering sleep restarts the loop phase so
     * the breathing cycle always begins from the neutral keyframe rather than mid-breath.
     */
    public void setSleeping(boolean sleeping, long gameTime) {
        if (sleeping && !this.sleeping) {
            this.sleepStartGameTime = gameTime;
        }
        this.sleeping = sleeping;
    }

    public void onMotionChanged(@Nonnull AnimationArchetype newArchetype,
                                byte newGeneration,
                                @Nonnull AnimationSelectionContext context,
                                long gameTime) {
        if (newArchetype == this.lastSeenArchetype && newGeneration == this.lastSeenGeneration) {
            return;
        }

        AnimationArchetype previousArchetype = this.lastSeenArchetype;
        boolean generationChanged = newGeneration != this.lastSeenGeneration;
        KeyframeAnimation resolvedAnimation = this.animationResolver.resolve(newArchetype, context);
        if (newArchetype == AnimationArchetype.IDLE) {
            this.layerStack.clearAction();
        } else if (generationChanged) {
            this.layerStack.triggerAction(resolvedAnimation, gameTime);
        } else {
            this.layerStack.setSustainedAction(resolvedAnimation, gameTime);
        }
        this.lastSeenArchetype = newArchetype;
        this.lastSeenGeneration = newGeneration;
        this.lastResolvedContext = context;

        log.debug("VillagerAnimator: motion change {} -> {} (gen={}, context: {}), resolved '{}' [blend in: {} ticks, loop: {}]",
                previousArchetype, newArchetype, newGeneration,
                context.mainHandCategory(),
                resolvedAnimation.getId(),
                resolvedAnimation.getBlendInTicks(),
                resolvedAnimation.getLoopMode());
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

        if (this.lastSeenArchetype == AnimationArchetype.IDLE || !this.layerStack.hasAction()) {
            return;
        }

        KeyframeAnimation reresolved = this.animationResolver.resolve(this.lastSeenArchetype, context);
        this.layerStack.setSustainedAction(reresolved, gameTime);

        log.debug("VillagerAnimator: context update for archetype {} (main hand: {}), resolved '{}'",
                this.lastSeenArchetype, context.mainHandCategory(), reresolved.getId());
    }

    /**
     * Arm config is a discrete render state that snaps to the incoming animation at transition
     * rather than blending — geometry visibility cannot be crossfaded, so we always read the
     * current (not outgoing) animation's declared config.
     */
    public ArmConfiguration currentArmConfiguration() {
        return this.currentArmConfiguration(0L, 0.0F);
    }

    public ArmConfiguration currentArmConfiguration(long gameTime, float partialTicks) {
        if (this.sleeping) {
            return this.sleepArmConfiguration(gameTime, partialTicks);
        }
        return this.layerStack.armConfiguration(gameTime, partialTicks);
    }

    public ArmConfiguration currentArmConfiguration(long gameTime,
                                                    float partialTicks,
                                                    @Nonnull LocomotionAnimationContext locomotionContext) {
        if (this.sleeping) {
            return this.sleepArmConfiguration(gameTime, partialTicks);
        }
        return this.layerStack.armConfiguration(gameTime, partialTicks, locomotionContext);
    }

    public AnimationFrame sample(long gameTime, float partialTicks) {
        if (this.sleeping) {
            return this.sleepAnimation.sample(this.sleepElapsedTicks(gameTime, partialTicks));
        }
        return this.layerStack.sample(gameTime, partialTicks);
    }

    public AnimationFrame sample(long gameTime,
                                 float partialTicks,
                                 @Nonnull LocomotionAnimationContext locomotionContext) {
        if (this.sleeping) {
            return this.sleepAnimation.sample(this.sleepElapsedTicks(gameTime, partialTicks));
        }
        return this.layerStack.sample(gameTime, partialTicks, locomotionContext);
    }

    private ArmConfiguration sleepArmConfiguration(long gameTime, float partialTicks) {
        return this.sleepAnimation.armConfigurationAt(this.sleepElapsedTicks(gameTime, partialTicks))
                .orElse(ArmConfiguration.BOTH_CROSSED);
    }

    private float sleepElapsedTicks(long gameTime, float partialTicks) {
        return Math.max(0.0F, (gameTime - this.sleepStartGameTime) + partialTicks);
    }

}
