package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.entities.ISettlementsBrainEntity;
import dev.breezes.settlements.logging.ILogger;
import dev.breezes.settlements.models.conditions.ICondition;
import dev.breezes.settlements.models.misc.ITickable;
import lombok.Getter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@Getter
public abstract class AbstractBehavior<T extends Entity & ISettlementsBrainEntity> implements IBehavior<T> {

    private final ILogger log;

    protected final ITickable preconditionCheckCooldown;
    protected final ITickable behaviorCoolDown;

    protected final List<ICondition<T>> preconditions;
    protected final List<ICondition<T>> continueConditions;

    protected BehaviorStatus status;
    private boolean stopRequested;

    protected AbstractBehavior(@Nonnull ILogger log,
                               @Nonnull ITickable preconditionCheckCooldown,
                               @Nonnull ITickable behaviorCoolDown) {
        this.log = log;
        this.preconditionCheckCooldown = preconditionCheckCooldown;
        this.behaviorCoolDown = behaviorCoolDown;

        // These fields should be initialized in the constructor of the subclass
        this.preconditions = new ArrayList<>();
        this.continueConditions = new ArrayList<>();

        this.status = BehaviorStatus.STOPPED;
        this.stopRequested = false;
    }

    @Override
    public boolean tickPreconditions(int delta, @Nonnull Level world, @Nonnull T entity) {
        // Only check preconditions & tick cooldowns if the behavior is not running
        if (this.status != BehaviorStatus.STOPPED) {
            log.behaviorTrace("Behavior is not stopped, skipping precondition check");
            return false;
        }

        // Check behavior cooldowns first
        // - we don't need to check preconditions if the behavior is on cooldown
        // - but we still need to tick the precondition check cooldown
        boolean preconditionCheckCooldownComplete = this.preconditionCheckCooldown.tickAndCheck(delta);
        if (!this.behaviorCoolDown.tickAndCheck(delta)) {
            log.behaviorTrace("Behavior is cooling down with {} remaining", this.behaviorCoolDown.getRemainingCooldownsAsPrettyString());
            return false;
        } else if (!preconditionCheckCooldownComplete) {
            log.behaviorTrace("Precondition check is cooling down, with {} remaining", this.preconditionCheckCooldown.getRemainingCooldownsAsPrettyString());
            return false;
        }

        // Reset precondition check cooldown
        this.preconditionCheckCooldown.reset();

        // Loop through all preconditions and check if they are all met
        for (ICondition<T> precondition : this.preconditions) {
            if (!precondition.test(entity)) {
                log.behaviorTrace("Precondition '{}' is not met", precondition.getClass().getSimpleName());
                return false;
            }
        }

        log.behaviorTrace("All preconditions are met");
        return true;
    }

    @Override
    public final void start(@Nonnull Level world, @Nonnull T entity) {
        if (this.status != BehaviorStatus.STOPPED) {
            log.warn("Ignoring start request since behavior is already running");
            return;
        }

        log.behaviorStatus("Starting behavior");
        this.doStart(world, entity);
        this.status = BehaviorStatus.RUNNING;

        // Reset behavior cooldown
        this.behaviorCoolDown.reset();
    }

    public abstract void doStart(@Nonnull Level world, @Nonnull T entity);

    @Override
    public final void tick(int delta, @Nonnull Level world, @Nonnull T entity) {
        if (this.status == BehaviorStatus.STOPPED) {
            return;
        }

        // Check for the stop flag, we want to early return since preconditions may no longer be met
        if (this.stopRequested) {
            log.behaviorStatus("Stop requested, stopping behavior");
            this.stop(world, entity);
            return;
        }

        log.behaviorTrace("Ticking behavior with delta {}", delta);
        try {
            this.tickBehavior(delta, world, entity);
        } catch (StopBehaviorException e) {
            log.behaviorStatus("Behavior stop requested by exception");
            this.requestStop();
        }

        // No cooldown for checking continue conditions
        boolean canContinue = !this.stopRequested && this.tickContinueConditions(delta, world, entity);
        if (!canContinue) {
            log.behaviorStatus("Behavior can no longer continue, stopping behavior");
            this.stop(world, entity);
        }
    }

    public abstract void tickBehavior(int delta, @Nonnull Level world, @Nonnull T entity);

    public boolean tickContinueConditions(int delta, @Nonnull Level world, @Nonnull T entity) {
        for (ICondition<T> continueCondition : this.continueConditions) {
            if (!continueCondition.test(entity)) {
                log.behaviorTrace("Continue condition '{}' is not met", continueCondition.getClass().getSimpleName());
                return false;
            }
        }
        log.behaviorTrace("All continue conditions are met");
        return true;
    }

    @Override
    public void requestStop() {
        if (this.stopRequested) {
            log.behaviorTrace("Ignoring stop request since behavior is already stopping");
            return;
        }

        log.behaviorStatus("Requesting to stop behavior");
        this.stopRequested = true;
    }

    @Override
    public final void stop(@Nonnull Level world, @Nonnull T entity) {
        if (this.status == BehaviorStatus.STOPPED) {
            log.warn("Ignoring stop request since behavior is already stopped");
            return;
        }

        log.behaviorStatus("Stopping behavior");
        this.doStop(world, entity);
        this.status = BehaviorStatus.STOPPED;
        this.stopRequested = false;
    }

    public abstract void doStop(@Nonnull Level world, @Nonnull T entity);

}
