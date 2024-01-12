package dev.breezes.settlements.entities.villager.animation.animator;

import com.mojang.logging.LogUtils;
import dev.breezes.settlements.entities.villager.BaseVillager;
import lombok.Getter;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.AnimationState;
import org.slf4j.Logger;

/**
 * A class that listens to the synced data (condition) and plays/stops the specified animation accordingly
 */
public abstract class BaseAnimator {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long DO_NOT_STOP = -1;

    protected final SynchedEntityData entityData;
    /**
     * The duration of the animation in milliseconds
     */
    private final long animationDuration;
    private final boolean stopImmediately;
    @Getter
    private final AnimationState animationState;

    /**
     * The timestamp that the animation should stop at/after
     * - if less than 0, the animation will not stop
     */
    protected long stopTime;

    public BaseAnimator(BaseVillager villager, float animationDurationSeconds, boolean stopImmediately) {
        this.entityData = villager.getEntityData();
        this.animationDuration = (long) (animationDurationSeconds * 1000);
        this.stopImmediately = stopImmediately;
        this.animationState = new AnimationState();

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
                LOGGER.info("Reset delayed-stop flag @ {} ticks", tickCount);
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
                if (this.animationState.getAccumulatedTime() > this.stopTime) {
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
            LOGGER.warn("Ignoring request to set stop flag for an animation that is not playing");
            return;
        }

        // Calculate & set stop time
        long runningTime = this.animationState.getAccumulatedTime();
        long currentIterations = Math.floorDiv(runningTime, this.animationDuration);
        this.stopTime = (currentIterations + 1) * this.animationDuration;
        LOGGER.info("Set delayed-stop flag @ {} ticks", tickCount);
    }

    protected void start(int tickCount) {
        if (this.isAnimationPlaying()) {
            LOGGER.warn("Ignoring request to start an animation that is already playing");
            return;
        }

        this.stopTime = DO_NOT_STOP;

        this.animationState.start(tickCount);
        LOGGER.info("Started animation @ {} ticks", tickCount);
    }

    protected void stop(int tickCount) {
        if (!this.isAnimationPlaying()) {
            LOGGER.warn("Ignoring request to stop an animation that is not playing");
            return;
        }

        this.animationState.stop();
        LOGGER.info("Stopped animation @ {} ticks", tickCount);
    }


    public boolean isAnimationPlaying() {
        return this.animationState.isStarted();
    }

}
