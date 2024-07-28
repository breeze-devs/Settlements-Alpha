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
    public void tick(int delta, @Nonnull Level world, @Nonnull T entity) {
        if (this.status == BehaviorStatus.STOPPED) {
            // Behavior is not running
            if (this.preconditionCheckCooldown.tickAndCheck(delta)) {
                log.debug("Checking behavior preconditions");
                this.tickPreconditions(delta, world, entity);
            }
        } else {
            // Behavior is running
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
            this.tickContinueConditions(delta, world, entity);
        }
    }

    public void tickPreconditions(int delta, @Nonnull Level world, @Nonnull T entity) {
        for (IEntityCondition<T> precondition : this.preconditions) {
            if (!precondition.test(entity)) {
                return;
            }
        }

        log.debug("All preconditions met, starting behavior");
        this.start(world, entity);
    }

    public void tickContinueConditions(int delta, @Nonnull Level world, @Nonnull T entity) {
        for (IEntityCondition<T> continueCondition : this.continueConditions) {
            if (!continueCondition.test(entity)) {
                log.debug("Continue condition %s not met, stopping behavior", continueCondition.getClass().getSimpleName());
                this.requestStop();
                return;
            }
        }
    }

    public abstract void tickBehavior(int delta, @Nonnull Level world, @Nonnull T entity);

    @Override
    public void start(@Nonnull Level world, @Nonnull T entity) {
        log.debug("Starting behavior");
        this.doStart(world, entity);
        this.status = BehaviorStatus.RUNNING;
    }

    public abstract void doStart(@Nonnull Level world, @Nonnull T entity);

    @Override
    public void requestStop() {
        log.debug("Requesting to stop behavior");
        this.stopRequested = true;
    }

    @Override
    public void stop(@Nonnull Level world, @Nonnull T entity) {
        log.debug("Stopping behavior");
        this.doStop(world, entity);
        this.status = BehaviorStatus.STOPPED;
        this.stopRequested = false;
    }

    public abstract void doStop(@Nonnull Level world, @Nonnull T entity);

}
