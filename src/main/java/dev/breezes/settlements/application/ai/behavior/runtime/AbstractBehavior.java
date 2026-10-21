package dev.breezes.settlements.application.ai.behavior.runtime;

import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.domain.ai.behavior.model.BehaviorStatus;
import dev.breezes.settlements.domain.ai.brain.ISettlementsBrainEntity;
import dev.breezes.settlements.domain.ai.conditions.ICondition;
import dev.breezes.settlements.domain.time.ITickable;
import dev.breezes.settlements.shared.logging.ILogger;
import lombok.AccessLevel;
import lombok.Getter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
public abstract class AbstractBehavior<T extends Entity & ISettlementsBrainEntity> implements IBehavior<T> {

    private final ILogger log;

    protected final ITickable preconditionCheckCooldown;
    protected final ITickable behaviorCoolDown;

    protected final List<ICondition<T>> preconditions;
    protected final List<ICondition<T>> continueConditions;

    protected BehaviorStatus status;
    private boolean stopRequested;
    /**
     * Reason of the current run's pending stop, or null while none is pending.
     */
    @Nullable
    @Getter(AccessLevel.NONE)
    private String stopReason;
    @Getter
    private final Map<Class<?>, Boolean> latestPreconditionEvaluationResults;

    protected AbstractBehavior(@Nonnull ILogger log,
                               @Nonnull ITickable preconditionCheckCooldown,
                               @Nonnull ITickable behaviorCoolDown) {
        this.log = log;
        this.preconditionCheckCooldown = preconditionCheckCooldown;
        this.behaviorCoolDown = behaviorCoolDown;

        // These fields should be initialized in the constructor of the subclass
        this.preconditions = new ArrayList<>();
        this.continueConditions = new ArrayList<>();
        this.latestPreconditionEvaluationResults = new LinkedHashMap<>();

        this.status = BehaviorStatus.STOPPED;
        this.stopRequested = false;
    }

    @Override
    public boolean tickPreconditions(int delta, @Nonnull Level world, @Nonnull T entity) {
        // Only check preconditions & tick cooldowns if the behavior is not running
        if (this.status != BehaviorStatus.STOPPED) {
            return false;
        }

        // Check behavior cooldowns first
        // - we don't need to check preconditions if the behavior is on cooldown
        // - but we still need to tick the precondition check cooldown
        boolean preconditionCheckCooldownComplete = this.preconditionCheckCooldown.tickAndCheck(delta);
        if (!this.behaviorCoolDown.tickAndCheck(delta)) {
            return false;
        } else if (!preconditionCheckCooldownComplete) {
            return false;
        }

        // Reset precondition check cooldown
        this.preconditionCheckCooldown.reset();
        this.latestPreconditionEvaluationResults.clear();

        if (this.preconditions.isEmpty()) {
            return true;
        }

        // Loop through all preconditions and check if they are all met
        boolean allPassed = true;
        for (ICondition<T> precondition : this.preconditions) {
            boolean passed = precondition.test(entity);
            this.latestPreconditionEvaluationResults.put(precondition.getClass(), passed);
            if (!passed) {
                log.trace("Precondition '{}' is not met", precondition.description());
                allPassed = false;
            }
        }

        return allPassed;
    }

    /**
     * Starts the behavior.
     * <p>
     * If doStart throws, the behavior is torn down as a stop would and the exception propagates.
     */
    @Override
    public final void start(@Nonnull Level world, @Nonnull T entity) {
        if (this.status != BehaviorStatus.STOPPED) {
            log.warn("Ignoring start request since behavior is already running");
            return;
        }

        log.debug("Starting behavior (entity: {})", entity.getUUID());
        try {
            this.doStart(world, entity);
        } catch (RuntimeException e) {
            this.tearDown(world, entity);
            throw e;
        }
        this.status = BehaviorStatus.RUNNING;
    }

    public abstract void doStart(@Nonnull Level world, @Nonnull T entity);

    protected double getCooldownMultiplier(@Nonnull T entity) {
        return 1.0;
    }

    @Override
    public final void tick(int delta, @Nonnull Level world, @Nonnull T entity) {
        if (this.status == BehaviorStatus.STOPPED) {
            return;
        }

        // Check for the stop flag, we want to early return since preconditions may no longer be met
        if (this.stopRequested) {
            log.trace("Stop requested, stopping behavior");
            this.stop(world, entity);
            return;
        }

        try {
            this.tickBehavior(delta, world, entity);
        } catch (StopBehaviorException e) {
            this.requestStop(e.getMessage());
        }

        // No cooldown for checking continue conditions
        boolean canContinue = !this.stopRequested && this.tickContinueConditions(delta, world, entity);
        if (!canContinue) {
            log.trace("Behavior can no longer continue, stopping behavior");
            this.stop(world, entity);
        }
    }

    public abstract void tickBehavior(int delta, @Nonnull Level world, @Nonnull T entity);

    /**
     * Returns whether every continue condition holds, requesting a stop that names the first unmet one otherwise.
     */
    public boolean tickContinueConditions(int delta, @Nonnull Level world, @Nonnull T entity) {
        for (ICondition<T> continueCondition : this.continueConditions) {
            if (!continueCondition.test(entity)) {
                this.requestStop("Continue condition '%s' is not met".formatted(continueCondition.description()));
                return false;
            }
        }

        return true;
    }

    @Override
    public void requestStop(@Nonnull String reason) {
        if (this.stopRequested) {
            return;
        }

        log.trace("Requesting to stop behavior: {}", reason);
        this.stopRequested = true;
        this.stopReason = reason;
    }

    @Override
    public final void stop(@Nonnull Level world, @Nonnull T entity) {
        if (this.status == BehaviorStatus.STOPPED) {
            log.warn("Ignoring stop request since behavior is already stopped");
            return;
        }

        // A stop without a pending request comes from whoever runs the behavior, not from the run itself
        log.debug("Stopping behavior (entity: {}, reason: {})", entity.getUUID(),
                Objects.requireNonNullElse(this.stopReason, "stopped by its runner"));
        this.tearDown(world, entity);
    }

    private void tearDown(@Nonnull Level world, @Nonnull T entity) {
        try {
            this.doStop(world, entity);
        } catch (Exception e) {
            // stop() runs on the entity-removal chain (BaseVillager.remove -> brain.stopAll -> ...),
            // so a throw here must never escape: it would abort removal itself.
            log.error("Behavior teardown threw for entity {}", entity.getUUID(), e);
        } finally {
            // Always advance to STOPPED so the behavior cannot be stuck as RUNNING
            this.behaviorCoolDown.resetWithMultiplier(this.getCooldownMultiplier(entity));
            this.status = BehaviorStatus.STOPPED;
            this.stopRequested = false;
            this.stopReason = null;
        }
    }

    public abstract void doStop(@Nonnull Level world, @Nonnull T entity);

}
