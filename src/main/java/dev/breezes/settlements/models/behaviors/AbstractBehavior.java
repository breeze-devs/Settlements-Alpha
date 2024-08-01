package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.entities.ISettlementsBrainEntity;
import dev.breezes.settlements.logging.ILogger;
import dev.breezes.settlements.models.conditions.IEntityCondition;
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
    protected final ITickable behaviorTickCooldown;

    protected final List<IEntityCondition<T>> preconditions;
    protected final List<IEntityCondition<T>> continueConditions;

    protected BehaviorStatus status;
    private boolean stopRequested;

    protected AbstractBehavior(@Nonnull ILogger log,
                               @Nonnull ITickable preconditionCheckCooldown,
                               @Nonnull ITickable behaviorCoolDown,
                               @Nonnull ITickable behaviorTickCooldown) {
        this.log = log;
        this.preconditionCheckCooldown = preconditionCheckCooldown;
        this.behaviorCoolDown = behaviorCoolDown;
        this.behaviorTickCooldown = behaviorTickCooldown;

        // These fields should be initialized in the constructor of the subclass
        this.preconditions = new ArrayList<>();
        this.continueConditions = new ArrayList<>();

        this.status = BehaviorStatus.STOPPED;
        this.stopRequested = false;
    }

    @Override
    public boolean tickPreconditions(int delta, @Nonnull Level world, @Nonnull T entity) {
        // TODO: do we want to also tick the behavior cooldown here
        if (this.preconditionCheckCooldown.tickCheckAndReset(delta)) {
            return false;
        }
        if (this.status != BehaviorStatus.STOPPED) {
            log.debug("Behavior is not stopped, skipping precondition check");
            return false;
        }

        // Loop through all preconditions and check if they are all met
        for (IEntityCondition<T> precondition : this.preconditions) {
            if (!precondition.test(entity)) {
                log.debug("Precondition '%s' is not met", precondition.getClass().getSimpleName());
                return false;
            }
        }

        log.debug("All preconditions are met");
        return true;
    }

    @Override
    public final void start(@Nonnull Level world, @Nonnull T entity) {
        if (this.status != BehaviorStatus.STOPPED) {
            log.warn("Ignoring start request since behavior is already running");
            return;
        }

        log.debug("Starting behavior");
        this.doStart(world, entity);
        this.status = BehaviorStatus.RUNNING;
    }

    public abstract void doStart(@Nonnull Level world, @Nonnull T entity);

    @Override
    public final void tick(int delta, @Nonnull Level world, @Nonnull T entity) {
        if (this.status == BehaviorStatus.STOPPED) {
            return;
        }

        // Check for the stop flag, we want to early return since preconditions may no longer be met
        if (this.stopRequested) {
            log.debug("Stop requested, stopping behavior");
            this.stop(world, entity);
            return;
        }

        if (this.behaviorTickCooldown.tickAndCheck(delta)) {
            log.debug("Ticking behavior with delta %d", delta);
            this.tickBehavior(delta, world, entity);
        }

        // No cooldown for checking continue conditions
        boolean canContinue = this.tickContinueConditions(delta, world, entity);
        if (!canContinue) {
            this.requestStop();
        }
    }

    public abstract void tickBehavior(int delta, @Nonnull Level world, @Nonnull T entity);

    public boolean tickContinueConditions(int delta, @Nonnull Level world, @Nonnull T entity) {
        for (IEntityCondition<T> continueCondition : this.continueConditions) {
            if (!continueCondition.test(entity)) {
                log.debug("Continue condition '%s' is not met", continueCondition.getClass().getSimpleName());
                return false;
            }
        }
        log.debug("All continue conditions are met");
        return true;
    }

    @Override
    public void requestStop() {
        log.debug("Requesting to stop behavior");
        this.stopRequested = true;
    }

    @Override
    public final void stop(@Nonnull Level world, @Nonnull T entity) {
        if (this.status == BehaviorStatus.STOPPED) {
            log.warn("Ignoring stop request since behavior is already stopped");
            return;
        }

        log.debug("Stopping behavior");
        this.doStop(world, entity);
        this.status = BehaviorStatus.STOPPED;
        this.stopRequested = false;
    }

    public abstract void doStop(@Nonnull Level world, @Nonnull T entity);

}
