package dev.breezes.settlements.entities.villager.animations.animator;

import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.entities.villager.animations.definitions.VillagerAnimationDefinition;
import dev.breezes.settlements.util.RandomUtil;
import lombok.CustomLog;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.AnimationState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A class that listens to the synced data (condition) and plays/stops the specified animation accordingly
 */
@CustomLog
public abstract class BaseAnimator {

    private static final long DO_NOT_STOP = -1;

    protected final SynchedEntityData entityData;
    private final boolean stopImmediately;

    /**
     * Map of individual animation definitions to their states
     */
    private final Map<VillagerAnimationDefinition, AnimationState> animationStates;

    /**
     * The animation that's currently playing, null if nothing is playing
     */
    private VillagerAnimationDefinition currentAnimationDefinition;

    /**
     * The timestamp that the animation should stop at/after
     * - if less than 0, the animation will not stop
     */
    protected long stopTime;

    public BaseAnimator(BaseVillager villager, List<VillagerAnimationDefinition> animations, boolean stopImmediately) {
        this.entityData = villager.getEntityData();

        this.stopImmediately = stopImmediately;

        // Initialize animation states for each animation definition
        this.animationStates = new HashMap<>();
        for (VillagerAnimationDefinition animation : animations) {
            this.animationStates.put(animation, new AnimationState());
        }

        this.currentAnimationDefinition = null;
        this.stopTime = DO_NOT_STOP;
    }

    /**
     * Checks the synced data condition to play/stop the animation
     * - called per-tick on the client side
     */
    public void tickAnimations(int tickCount) {
        boolean shouldAnimate = this.shouldAnimate();
        boolean isPlaying = this.isAnimationPlaying();

        // Check start animation conditions
        if (shouldAnimate) {
            // Start animation if not already playing
            if (!isPlaying) {
                this.start(tickCount);
            }

            // Reset the stop flag if set
            if (this.stopTime > 0) {
                this.stopTime = DO_NOT_STOP;
                log.info("Reset delayed-stop flag @ %d ticks", tickCount);
            }
        } else {
            // Stop animation if playing
            if (!isPlaying) {
                return;
            }

            // Check if we should stop immediately
            if (this.stopImmediately) {
                this.stop(tickCount);
            }
            // Check if we have a stop flag set
            else if (this.stopTime > 0) {
                // Delayed stop flag is set, check if we are past the stop time
                Optional<AnimationState> currentAnimationState = this.getCurrentState();
                if (currentAnimationState.isEmpty() || currentAnimationState.get().getAccumulatedTime() > this.stopTime) {
                    this.stop(tickCount);
                }
            }
            // Neither, set the stop flag
            else {
                this.setStopFlag(tickCount);
            }
        }
    }

    protected abstract boolean shouldAnimate();

    protected void setStopFlag(int tickCount) {
        if (!this.isAnimationPlaying()) {
            log.warn("Ignoring request to set stop flag for an animation that is not playing");
            return;
        }

        Optional<AnimationState> currentAnimationState = this.getCurrentState();
        if (currentAnimationState.isEmpty()) {
            log.warn("Current animation state is null when setting stop flag");
            this.stopTime = 0; // stop immediately
        } else {
            // Calculate & set stop time
            long runningTime = currentAnimationState.get().getAccumulatedTime();
            long animationDuration = (long) (this.currentAnimationDefinition.definition().lengthInSeconds() * 1000);
            long currentIterations = Math.floorDiv(runningTime, animationDuration);
            this.stopTime = (currentIterations + 1) * animationDuration;
        }

        log.info("Set delayed-stop flag @ %d ticks", tickCount);
    }

    protected void start(int tickCount) {
        if (this.isAnimationPlaying()) {
            log.warn("Ignoring request to start an animation that is already playing");
            return;
        }

        this.stopTime = DO_NOT_STOP;

        this.currentAnimationDefinition = this.randomAnimation();
        this.animationStates.get(this.currentAnimationDefinition).start(tickCount);

        log.info("Started animation @ %d ticks", tickCount);
    }

    protected void stop(int tickCount) {
        if (!this.isAnimationPlaying()) {
            log.warn("Ignoring request to stop an animation that is not playing");
            return;
        }

        Optional<AnimationState> currentAnimationState = this.getCurrentState();
        if (currentAnimationState.isEmpty()) {
            log.error("Cancelling stop request since current animation state is null");
            return;
        }

        currentAnimationState.get().stop();
        log.info("Stopped animation @ %d ticks", tickCount);
    }

    public boolean isAnimationPlaying() {
        Optional<AnimationState> currentAnimation = this.getCurrentState();
        return currentAnimation.isPresent() && currentAnimation.get().isStarted();
    }

    private VillagerAnimationDefinition randomAnimation() {
        VillagerAnimationDefinition animation = RandomUtil.choice(this.animationStates.keySet().stream().toList());
        log.info("Randomly selected animation: %s", animation.name());
        return animation;
    }

    public Optional<AnimationState> getCurrentState() {
        if (this.currentAnimationDefinition == null) {
            return Optional.empty();
        }
        return Optional.of(this.animationStates.get(this.currentAnimationDefinition));
    }

    public Optional<AnimationDefinition> getCurrentDefinition() {
        return Optional.ofNullable(this.currentAnimationDefinition).map(VillagerAnimationDefinition::definition);
    }

}
