package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.application.ai.behavior.publication.BehaviorOutcomePublisher;
import dev.breezes.settlements.application.ai.override.OverridePolicy;
import dev.breezes.settlements.application.ai.override.OverrideRequest;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.domain.ai.behavior.model.BehaviorStatus;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.catalog.BehaviorPlanningMetadata;
import dev.breezes.settlements.domain.ai.catalog.IBehaviorCatalog;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.ai.planning.IAsyncPlanGenerator;
import dev.breezes.settlements.domain.ai.planning.IPlanGenerator;
import dev.breezes.settlements.domain.ai.planning.PinnedSelection;
import dev.breezes.settlements.domain.ai.planning.PlanArrival;
import dev.breezes.settlements.domain.ai.planning.PlanAuthor;
import dev.breezes.settlements.domain.ai.planning.PlanGenerationContext;
import dev.breezes.settlements.domain.ai.planning.PlanIntent;
import dev.breezes.settlements.domain.ai.planning.PlanSlot;
import dev.breezes.settlements.domain.ai.planning.PlanSlotStatus;
import dev.breezes.settlements.domain.ai.planning.PlanStatus;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventEmitter;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.WorldCalendar;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.annotations.stylistic.VisibleForTesting;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Application-layer entry point for executing villager day plans.
 */
@ServerScope
@CustomLog
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class PlanRunner {

    private static final String RESET_REASON_MISSING_PLAN = "missing plan";
    private static final String RESET_REASON_MISSING_SUCCESSOR = "missing successor plan";
    private static final String RESET_REASON_ASYNC_OVERRUN = "async plan generation overrun";
    private static final String RESET_REASON_BACKWARD_JUMP = "backward dayTime jump";
    private static final String RESET_REASON_BACKWARD_WHILE_UNLOADED = "backward dayTime jump while unloaded";
    private static final String RESET_REASON_CALENDAR_DAY_MISMATCH = "calendar day mismatch";

    private static final int OVERRIDE_TICK_DELTA = 1;

    /**
     * Fallback safety-net ceiling on how long a single override may run, used when the active
     * override's key has no catalog descriptor (see {@link #resolveOverrideMaxDurationTicks}).
     * Overrides are reactive and short-lived by design; one that runs past its ceiling has wedged
     * (e.g. mirroring a trade/courtship session that never closes, or navigating to a target that
     * never becomes reachable). When the ceiling trips, the override is force-stopped and the
     * interrupted plan slot re-queued so the villager resumes its day instead of standing frozen
     * until a panic clears it.
     */
    private static final int MAX_OVERRIDE_DURATION_TICKS = ClockTicks.seconds(120).getTicksAsInt();

    /**
     * Default safety-net ceiling on how long a single plan-slot behavior may run. Matches the
     * override ceiling by design — both are two-minute backstops. Behaviors that legitimately
     * run longer (e.g. manage-chests, cultivate-plot) should declare a higher
     * {@link BehaviorPlanningMetadata#getMaxRunDuration()} to avoid premature abort.
     */
    private static final int DEFAULT_MAX_BEHAVIOR_RUN_TICKS = ClockTicks.seconds(120).getTicksAsInt();

    /**
     * Spacing between successive attempts to start a rigid plan slot whose preconditions are not yet satisfied.
     */
    private static final int SLOT_START_RETRY_INTERVAL_TICKS = ClockTicks.seconds(0.5).getTicksAsInt();

    private static final Comparator<OverridePolicy> OVERRIDE_POLICY_PRECEDENCE = Comparator
            .comparingInt(OverridePolicy::priority)
            .reversed()
            .thenComparing(policy -> policy.getClass().getName());

    private final IBehaviorCatalog catalog;
    private final PlanGenerationContextFactory planGenerationContextFactory;
    private final IPlanGenerator planGenerator;
    private final IAsyncPlanGenerator asyncPlanGenerator;
    private final VillagerWakeScheduler wakeScheduler;
    private final MinimalPlanFactory minimalPlanFactory;
    private final Set<OverridePolicy> overridePolicies;
    private final BehaviorOutcomePublisher behaviorOutcomePublisher;
    private final WorldEventEmitter worldEventEmitter;
    private final PlanRequestService planRequestService;

    /**
     * TODO:CONFIRM -- would this also be useful for player injecting an override? e.g. test/debug command to tell
     *    farmer to harvest pumpkin right now; or in the future, conversation & player suggests to do X behavior rn
     * Evaluates and ticks the override slot. Must be called at the top of the behavior
     * wrapper's tick, before the activity gate, so reactive accepts fire under any brain
     * Activity (WORK, MEET, IDLE, REST, etc.) and the plan slot never concurrently runs
     * alongside an active override.
     * <p>
     * Returns {@code true} when the override slot was active this tick, signaling the caller
     * to skip the normal plan tick entirely.
     */
    public boolean tickOverride(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        PlanRuntimeState runtime = villager.getPlanRuntimeState();

        if (runtime.isOverrideActive()) {
            this.tickActiveOverride(level, villager, runtime);
            return true;
        }

        return this.tryStartOverride(level, villager, runtime);
    }

    public void tick(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        PlanRuntimeState runtime = villager.getPlanRuntimeState();
        long dayTime = level.getDayTime();
        DeltaResult delta = runtime.advanceClock(dayTime);

        this.drainPendingArrivals(runtime, villager, dayTime);
        DayPlan adopted = this.adoptPendingIfReady(level, villager, runtime, dayTime);
        DayPlan plan = adopted != null ? adopted : villager.getDayPlan();

        if (this.shouldFallbackFromAsyncOverrun(runtime, dayTime)) {
            runtime.cancelPendingFuture();
            plan = this.hardReset(level, villager, runtime, dayTime, RESET_REASON_ASYNC_OVERRUN, delta);
        } else if (delta.backwardJump()) {
            // Backward jumps invalidate chronological slot ordering, so retrying the existing cursor would replay future intent in the past.
            plan = this.hardReset(level, villager, runtime, dayTime, RESET_REASON_BACKWARD_JUMP, delta);
        } else if (plan == null) {
            plan = this.hardReset(level, villager, runtime, dayTime, RESET_REASON_MISSING_PLAN, delta);
        } else if (runtime.isPlanExhausted()) {
            // Once exhausted, generation/adoption owns progress; ticking slots here just repeats cleanup work.
            return;
        } else if (this.isPlanOverdue(plan, dayTime)) {
            log.behaviorWarn("Plan overdue backstop fired for villager {}: dayTime={}, wakeAtAbsoluteTick={}",
                    villager.getUUID(), dayTime, plan.getWakeAtAbsoluteTick());
            this.completeExpiredPlanAndSubmitSuccessor(level, villager, runtime, plan);
            return;
        } else if (hasCalendarDayMismatch(plan, dayTime)) {
            // Forward time jumps (e.g. /time set, bed-sleep skip) can cross the calendar boundary
            // before the plan's authored duration elapses; without this gate the villager would
            // execute yesterday's plan into today.
            log.behaviorStatus("Plan calendar mismatch for villager {}: planDay={}, currentDay={}, dayTime={}",
                    villager.getUUID(), plan.getCalendarDay(), WorldCalendar.calendarDayOf(dayTime), dayTime);
            this.completeExpiredPlanAndSubmitSuccessor(level, villager, runtime, plan);
            return;
        } else if (delta.firstTick() && detectOnLoadBackward(plan, nowCivilFor(plan, dayTime))) {
            // Runtime clocks are transient across reloads; persisted slot order is the only reliable signal for unloaded time travel.
            plan = this.hardReset(level, villager, runtime, dayTime, RESET_REASON_BACKWARD_WHILE_UNLOADED, delta);
        } else if (delta.deltaTicks() == 0) {
            // Frozen daylight should freeze planned behavior too, otherwise villagers drift through schedules while the world clock is paused.
            return;
        } else {
            // Forward jumps should catch up pending schedule windows without interrupting an active behavior mid-execution.
            runSeekLoop(plan, nowCivilFor(plan, dayTime));
        }

        if (plan.isExhausted() || plan.getCurrentSlot().isEmpty()) {
            this.completeExpiredPlanAndSubmitSuccessor(level, villager, runtime, plan);
            return;
        }

        this.tickSlot(level, villager, plan, plan.getCurrentSlot().get(), runtime,
                delta.deltaTicks(), nowCivilFor(plan, dayTime));
    }

    public void ensureValidPlan(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        PlanRuntimeState runtime = villager.getPlanRuntimeState();
        long dayTime = level.getDayTime();
        this.drainPendingArrivals(runtime, villager, dayTime);
        DayPlan plan = this.adoptPendingIfReady(level, villager, runtime, dayTime);
        if (plan == null) {
            plan = villager.getDayPlan();
        }
        if (plan != null
                && !this.isPlanOverdue(plan, dayTime)
                && !hasCalendarDayMismatch(plan, dayTime)) {
            return;
        }

        DayPlan pendingNextPlan = runtime.getPendingNextPlan();
        if (shouldWaitForPendingPlan(pendingNextPlan, dayTime)) {
            return;
        }

        if (plan != null && pendingNextPlan == null) {
            long nextWakeAtAbsoluteTick = this.wakeScheduler.nextWakeAtAbsoluteTick(villager, dayTime, plan.getWakeAtAbsoluteTick());
            if (dayTime < nextWakeAtAbsoluteTick) {
                this.completeExpiredPlanAndSubmitSuccessor(level, villager, runtime, plan);
                return;
            }
        }

        String resetReason;
        if (plan == null) {
            resetReason = RESET_REASON_MISSING_PLAN;
        } else if (hasCalendarDayMismatch(plan, dayTime)) {
            resetReason = RESET_REASON_CALENDAR_DAY_MISMATCH;
        } else {
            resetReason = RESET_REASON_MISSING_SUCCESSOR;
        }
        this.hardReset(level, villager, runtime, dayTime, resetReason, singleTickDelta(runtime));
        log.behaviorStatus("Plan regenerated for villager {} during unmanaged activity: dayTime={}",
                villager.getUUID(), dayTime);
    }

    public void suspendIfActive(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        PlanRuntimeState runtime = villager.getPlanRuntimeState();
        IBehavior<BaseVillager> behavior = runtime.getCurrentBehavior();
        if (behavior == null || behavior.getStatus() == BehaviorStatus.STOPPED) {
            return;
        }

        // Synchronous stop so teardown obligations are discharged immediately
        behavior.stop(level, villager);

        DayPlan plan = villager.getDayPlan();
        if (plan != null) {
            plan.getCurrentSlot()
                    .filter(slot -> slot.getStatus() == PlanSlotStatus.ACTIVE)
                    .ifPresent(slot -> slot.markStatus(PlanSlotStatus.INTERRUPTED));
        }
        runtime.clearCurrentBehavior();
        this.clearPlanActiveMemory(villager);
    }

    public void forceStop(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        PlanRuntimeState runtime = villager.getPlanRuntimeState();

        // Stop the override slot first
        IBehavior<BaseVillager> override = runtime.getOverrideBehavior();
        if (override != null && override.getStatus() != BehaviorStatus.STOPPED) {
            override.stop(level, villager);
        }
        runtime.clearOverride();

        IBehavior<BaseVillager> behavior = runtime.getCurrentBehavior();
        if (behavior != null && behavior.getStatus() != BehaviorStatus.STOPPED) {
            behavior.stop(level, villager);
        }
        runtime.clearCurrentBehavior();
        this.clearPlanActiveMemory(villager);
    }

    private void tickSlot(@Nonnull ServerLevel level,
                          @Nonnull BaseVillager villager,
                          @Nonnull DayPlan plan,
                          @Nonnull PlanSlot slot,
                          @Nonnull PlanRuntimeState runtime,
                          int delta,
                          int nowCivil) {
        switch (slot.getStatus()) {
            case PENDING -> this.tryStartSlot(level, villager, plan, slot, runtime, delta, nowCivil);
            case ACTIVE -> this.tickActiveSlot(level, villager, plan, slot, runtime, delta);
            case COMPLETED, SKIPPED, INTERRUPTED -> {
                this.clearPlanActiveMemory(villager);
                plan.advanceSlot();
            }
        }
    }

    private void tryStartSlot(@Nonnull ServerLevel level,
                              @Nonnull BaseVillager villager,
                              @Nonnull DayPlan plan,
                              @Nonnull PlanSlot slot,
                              @Nonnull PlanRuntimeState runtime,
                              int delta,
                              int nowCivil) {
        // Throttle retries
        if (runtime.getSlotStartRetryDelayTicks() > 0) {
            runtime.decaySlotStartRetryDelay(delta);
            return;
        }

        if (!isSlotWindowOpen(slot, nowCivil)) {
            this.clearPlanActiveMemory(villager);
            return;
        }

        Optional<IBehavior<BaseVillager>> maybeBehavior = this.catalog.createBehavior(slot.getBehaviorKey());
        if (maybeBehavior.isEmpty()) {
            log.behaviorWarn("Plan slot skipped for villager {}: behavior '{}' missing from catalog",
                    villager.getUUID(), slot.getBehaviorKey());
            slot.markStatus(PlanSlotStatus.SKIPPED);
            this.clearPlanActiveMemory(villager);
            plan.advanceSlot();
            return;
        }

        IBehavior<BaseVillager> behavior = maybeBehavior.get();

        // The plan model creates a fresh behavior instance per slot (catalog factory, not cached),
        // so the per-instance RandomRangeTickable cooldowns start armed at their initial value and
        // would prevent the precondition check from running on the very first tick. forceComplete()
        // bypasses that: cadence is now enforced at plan-generation time (slot spacing in
        // WindowPacker), not at runtime.
        behavior.getBehaviorCoolDown().forceComplete();
        behavior.getPreconditionCheckCooldown().forceComplete();

        if (!behavior.tickPreconditions(1, level, villager)) {
            if (slot.isFlexible()) {
                log.behaviorTrace("Plan slot skipped for villager {}: behavior '{}' preconditions not met",
                        villager.getUUID(), slot.getBehaviorKey());
                slot.markStatus(PlanSlotStatus.SKIPPED);
                this.clearPlanActiveMemory(villager);
                plan.advanceSlot();
                return;
            }

            // Rigid slot keeps its window and retries later
            runtime.armSlotStartRetryDelay(SLOT_START_RETRY_INTERVAL_TICKS - 1);
            return;
        }

        try {
            behavior.start(level, villager);
        } catch (RuntimeException e) {
            // Never installed as the current behavior (assignPlanBehavior hasn't run yet), so
            // nothing further needs stopping here; just skip the slot rather than retry a start
            // that will likely fail again.
            log.behaviorError("Plan behavior '{}' threw during start for villager {}; skipping slot",
                    slot.getBehaviorKey(), villager.getUUID(), e);
            slot.markStatus(PlanSlotStatus.SKIPPED);
            this.clearPlanActiveMemory(villager);
            plan.advanceSlot();
            return;
        }
        runtime.assignPlanBehavior(behavior, this.catalog.getDescriptor(slot.getBehaviorKey()).orElse(null));
        this.setPlanActiveMemory(villager);
        slot.markStatus(PlanSlotStatus.ACTIVE);
        plan.markStatus(PlanStatus.ACTIVE);

        this.worldEventEmitter.emitBehaviorStarted(villager, slot.getBehaviorKey());
    }

    private void tickActiveSlot(@Nonnull ServerLevel level,
                                @Nonnull BaseVillager villager,
                                @Nonnull DayPlan plan,
                                @Nonnull PlanSlot slot,
                                @Nonnull PlanRuntimeState runtime,
                                int delta) {
        IBehavior<BaseVillager> behavior = runtime.getCurrentBehavior();
        if (behavior == null) {
            slot.markStatus(PlanSlotStatus.INTERRUPTED);
            this.clearPlanActiveMemory(villager);
            plan.advanceSlot();
            return;
        }

        runtime.incrementCurrentBehaviorElapsedTicks(delta);

        // Resolve the per-behavior ceiling from the descriptor when available; fall back to the
        // module-wide default so even behaviors without explicit metadata are always bounded.
        BehaviorPlanningMetadata descriptor = runtime.getCurrentDescriptor();
        int maxRunTicks = descriptor != null
                ? descriptor.getMaxRunDuration().getTicksAsInt()
                : DEFAULT_MAX_BEHAVIOR_RUN_TICKS;

        if (runtime.getCurrentBehaviorElapsedTicks() > maxRunTicks) {
            this.abortStuckPlanBehavior(level, villager, runtime, slot, plan, behavior);
            return;
        }

        try {
            behavior.tick(delta, level, villager);
        } catch (RuntimeException e) {
            log.behaviorError("Plan behavior '{}' threw during tick for villager {}; force-stopping wedged behavior",
                    slot.getBehaviorKey(), villager.getUUID(), e);
            this.abortPlanBehavior(level, villager, runtime, slot, plan, behavior, "behavior threw during tick");
            return;
        }

        if (behavior.getStatus() == BehaviorStatus.STOPPED) {
            slot.markStatus(PlanSlotStatus.COMPLETED);
            this.behaviorOutcomePublisher.publishCompleted(villager, slot.getBehaviorKey(), behavior);
            runtime.clearCurrentBehavior();
            this.clearPlanActiveMemory(villager);
            plan.advanceSlot();
        }
    }

    /**
     * Force-stops a plan-slot behavior that has exceeded its {@link BehaviorPlanningMetadata#getMaxRunDuration()}
     * ceiling (or {@link #DEFAULT_MAX_BEHAVIOR_RUN_TICKS} when no descriptor is present). Unlike the
     * override abort, this does NOT re-queue the slot — re-queuing would restart the same wedged
     * behavior and produce a one-minute stall loop. Instead the slot is marked SKIPPED and the cursor
     * advances, so the villager's remaining day continues.
     */
    private void abortStuckPlanBehavior(@Nonnull ServerLevel level,
                                        @Nonnull BaseVillager villager,
                                        @Nonnull PlanRuntimeState runtime,
                                        @Nonnull PlanSlot slot,
                                        @Nonnull DayPlan plan,
                                        @Nonnull IBehavior<BaseVillager> behavior) {
        log.behaviorWarn("Plan behavior '{}' exceeded max run duration ({} ticks) for villager {}; force-stopping wedged behavior",
                slot.getBehaviorKey(), runtime.getCurrentBehaviorElapsedTicks(), villager.getUUID());
        this.abortPlanBehavior(level, villager, runtime, slot, plan, behavior, "behavior ceiling exceeded");
    }

    /**
     * Shared recovery for a plan-slot behavior that cannot continue (run-duration ceiling or an
     * uncaught exception from tick): stop it, mark the slot SKIPPED rather than re-queue it (a
     * wedged or throwing behavior would just repeat the same failure next attempt), and publish
     * the failure under the caller-supplied reason.
     */
    private void abortPlanBehavior(@Nonnull ServerLevel level,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull PlanRuntimeState runtime,
                                   @Nonnull PlanSlot slot,
                                   @Nonnull DayPlan plan,
                                   @Nonnull IBehavior<BaseVillager> behavior,
                                   @Nonnull String failureReason) {
        if (behavior.getStatus() != BehaviorStatus.STOPPED) {
            behavior.stop(level, villager);
        }
        slot.markStatus(PlanSlotStatus.SKIPPED);
        runtime.clearCurrentBehavior();
        this.clearPlanActiveMemory(villager);
        plan.advanceSlot();
        this.behaviorOutcomePublisher.publishFailed(villager, slot.getBehaviorKey(), failureReason);
    }

    /**
     * Advances the running override behavior by one tick.
     * <p>
     * On completion the interrupted plan slot is restored to PENDING so the plan retries
     * it on the next tick if the time window is still open.
     */
    private void tickActiveOverride(@Nonnull ServerLevel level,
                                    @Nonnull BaseVillager villager,
                                    @Nonnull PlanRuntimeState runtime) {
        IBehavior<BaseVillager> override = runtime.getOverrideBehavior();
        if (override == null) {
            // Slot was cleared externally (e.g. panic forceStop); nothing to do.
            runtime.clearOverride();
            return;
        }

        runtime.incrementOverrideElapsedTicks(OVERRIDE_TICK_DELTA);
        int maxOverrideDurationTicks = this.resolveOverrideMaxDurationTicks(runtime.getOverrideBehaviorKey());
        if (runtime.getOverrideElapsedTicks() > maxOverrideDurationTicks) {
            this.abortStuckOverride(level, villager, runtime, override, maxOverrideDurationTicks);
            return;
        }

        try {
            override.tick(OVERRIDE_TICK_DELTA, level, villager);
        } catch (RuntimeException e) {
            log.behaviorError("Override '{}' threw during tick for villager {}; force-stopping wedged override",
                    runtime.getOverrideBehaviorKey(), villager.getUUID(), e);
            this.abortOverride(level, villager, runtime, override);
            return;
        }

        if (override.getStatus() == BehaviorStatus.STOPPED) {
            this.onOverrideCompleted(villager, runtime, runtime.getOverrideBehaviorKey(), override);
        }
    }

    /**
     * Force-stops an override that has exceeded its per-behavior ceiling (see
     * {@link #resolveOverrideMaxDurationTicks}). Unlike a normal completion this publishes no
     * outcome — the override never finished its work — but it still discharges teardown via
     * {@code stop()}, clears the slot and the plan-active lock, and re-queues the interrupted plan
     * slot so the villager resumes instead of remaining frozen.
     */
    private void abortStuckOverride(@Nonnull ServerLevel level,
                                    @Nonnull BaseVillager villager,
                                    @Nonnull PlanRuntimeState runtime,
                                    @Nonnull IBehavior<BaseVillager> override,
                                    int maxOverrideDurationTicks) {
        log.behaviorWarn("Override '{}' exceeded max duration ({} ticks) for villager {}; force-stopping wedged override",
                runtime.getOverrideBehaviorKey(), maxOverrideDurationTicks, villager.getUUID());
        this.abortOverride(level, villager, runtime, override);
    }

    /**
     * Resolves the active override's run-duration ceiling from its catalog descriptor — the same
     * per-behavior mechanism {@link #tickActiveSlot} uses for plan-slot behaviors — so accept-style
     * overrides (e.g. TRADE_ACCEPT, COURTSHIP_ACCEPT) are not clipped to the generic override
     * fallback. Falls back to {@link #MAX_OVERRIDE_DURATION_TICKS} when the key is null or absent
     * from the catalog (accept behaviors are pool-absent but still catalog-present, so this should
     * only bite on a genuinely unregistered key).
     */
    private int resolveOverrideMaxDurationTicks(@Nullable BehaviorKey overrideBehaviorKey) {
        if (overrideBehaviorKey == null) {
            return MAX_OVERRIDE_DURATION_TICKS;
        }
        return this.catalog.getDescriptor(overrideBehaviorKey)
                .map(BehaviorPlanningMetadata::getMaxRunDuration)
                .map(ClockTicks::getTicksAsInt)
                .orElse(MAX_OVERRIDE_DURATION_TICKS);
    }

    /**
     * Shared recovery for an override that cannot continue (run-duration ceiling or an uncaught
     * exception from tick): stop it, clear the slot, and re-queue the plan behavior it interrupted
     * so the villager resumes its day instead of remaining frozen.
     */
    private void abortOverride(@Nonnull ServerLevel level,
                               @Nonnull BaseVillager villager,
                               @Nonnull PlanRuntimeState runtime,
                               @Nonnull IBehavior<BaseVillager> override) {
        if (override.getStatus() != BehaviorStatus.STOPPED) {
            override.stop(level, villager);
        }
        runtime.clearOverride();
        this.clearPlanActiveMemory(villager);
        this.reAttemptInterruptedSlot(villager);
    }

    /**
     * Handles override completion: publishes the completion outcome and re-queues the interrupted
     * plan slot so the villager resumes its day where the override pre-empted it.
     */
    private void onOverrideCompleted(@Nonnull BaseVillager villager,
                                     @Nonnull PlanRuntimeState runtime,
                                     @Nullable BehaviorKey completedKey,
                                     @Nonnull IBehavior<BaseVillager> completed) {
        if (completedKey != null) {
            this.behaviorOutcomePublisher.publishCompleted(villager, completedKey, completed);
        } else {
            log.behaviorWarn("Override completed without behavior key for villager {}", villager.getUUID());
        }
        runtime.clearOverride();

        log.behaviorStatus("Override '{}' completed for villager {}; re-queuing interrupted plan slot", completedKey, villager.getUUID());
        this.reAttemptInterruptedSlot(villager);
    }

    /**
     * Evaluates pending override triggers by explicit policy priority. When one is found,
     * suspends the active plan behavior, installs the override slot, and starts it.
     * <p>
     * Returns {@code true} if an override was installed (caller must skip the plan tick),
     * {@code false} when no policy fires.
     */
    private boolean tryStartOverride(@Nonnull ServerLevel level,
                                     @Nonnull BaseVillager villager,
                                     @Nonnull PlanRuntimeState runtime) {
        if (!canInterruptCurrentPlanBehavior(runtime.getCurrentDescriptor())) {
            return false;
        }

        OverrideRequest request = null;
        for (OverridePolicy policy : orderedOverridePolicies(this.overridePolicies)) {
            Optional<OverrideRequest> maybeRequest = policy.evaluate(level, villager);
            if (maybeRequest.isPresent()) {
                request = maybeRequest.get();
                break;
            }
        }

        if (request == null) {
            return false;
        }

        BehaviorKey key = request.getBehaviorKey();
        IBehavior<BaseVillager> behavior = catalog.createBehavior(key).orElse(null);
        if (behavior == null) {
            log.behaviorWarn("Override behavior '{}' not found in catalog for villager {}", key, villager.getUUID());
            return false;
        }

        behavior.getBehaviorCoolDown().forceComplete();
        behavior.getPreconditionCheckCooldown().forceComplete();

        if (!behavior.tickPreconditions(OVERRIDE_TICK_DELTA, level, villager)) {
            // Policy fired but preconditions not yet met. The policy polls every tick, so the
            // still-live trigger is re-evaluated next tick — nothing is consumed.
            log.behaviorTrace("Override preconditions not met for '{}' on villager {}", key, villager.getUUID());
            return false;
        }

        // Suspend the running plan behavior so navigation and teardown obligations are
        // discharged before we hand the body to the override.
        this.suspendIfActive(level, villager);
        this.reAttemptInterruptedSlot(villager);

        try {
            behavior.start(level, villager);
        } catch (RuntimeException e) {
            // The interrupted plan slot was already re-queued to PENDING above, so the villager
            // resumes its day normally next tick; never installed as the override, so nothing
            // further needs stopping here.
            log.behaviorError("Override behavior '{}' threw during start for villager {}; skipping override",
                    key, villager.getUUID(), e);
            return false;
        }
        runtime.installOverride(behavior, key);
        this.setPlanActiveMemory(villager);
        log.behaviorStatus("Override '{}' installed for villager {}", key, villager.getUUID());
        return true;
    }

    @VisibleForTesting
    static boolean canInterruptCurrentPlanBehavior(@Nullable BehaviorPlanningMetadata descriptor) {
        return descriptor == null || descriptor.isInterruptible();
    }

    @VisibleForTesting
    static List<OverridePolicy> orderedOverridePolicies(@Nonnull Collection<OverridePolicy> policies) {
        return policies.stream().sorted(OVERRIDE_POLICY_PRECEDENCE).toList();
    }

    /**
     * Flips the current plan slot from INTERRUPTED back to PENDING so the plan runner
     * retries the same work on the next ordinary tick.
     * <p>
     * If the slot's time window has since closed, it will be skipped by the seek loop
     * that runs at the start of each ordinary plan tick — no special handling needed here.
     */
    private void reAttemptInterruptedSlot(@Nonnull BaseVillager villager) {
        DayPlan plan = villager.getDayPlan();
        if (plan == null) {
            return;
        }
        plan.getCurrentSlot()
                .filter(slot -> slot.getStatus() == PlanSlotStatus.INTERRUPTED)
                .ifPresent(slot -> slot.markStatus(PlanSlotStatus.PENDING));
    }

    /**
     * A one-tick-forward {@link DeltaResult} for hard resets triggered outside the normal clock
     * advance (unmanaged-activity revalidation, override-confirm regeneration): no backward jump,
     * not the first tick — just "advance by one and regenerate".
     */
    private static DeltaResult singleTickDelta(@Nonnull PlanRuntimeState runtime) {
        return DeltaResult.builder()
                .deltaTicks(1)
                .backwardJump(false)
                .firstTick(false)
                .rawDelta(0L)
                .previousDayTime(runtime.getPreviousPlanTickDayTime())
                .build();
    }

    private DayPlan generatePlan(@Nonnull ServerLevel level,
                                 @Nonnull BaseVillager villager,
                                 long wakeAtAbsoluteTick,
                                 @Nonnull PlanIntent intent) {
        return this.planGenerator.generate(this.createGenerationContext(villager, wakeAtAbsoluteTick), intent);
    }

    private PlanGenerationContext createGenerationContext(@Nonnull BaseVillager villager, long wakeAtAbsoluteTick) {
        return this.planGenerationContextFactory.create(villager, wakeAtAbsoluteTick);
    }

    private DayPlan regeneratePlan(@Nonnull ServerLevel level,
                                   @Nonnull BaseVillager villager,
                                   long dayTime,
                                   @Nonnull PlanIntent intent) {
        long wakeAtAbsoluteTick = currentWakeAtOrBefore(villager, dayTime);
        DayPlan newPlan;
        try {
            newPlan = this.generatePlan(level, villager, wakeAtAbsoluteTick, intent);
        } catch (Exception exception) {
            // A hard reset must yield a plan THIS tick — the caller immediately seeks and ticks it.
            // The floor does not accept an intent, so carried pins are acceptably lost on this path.
            newPlan = this.minimalPlanFactory.create(villager, wakeAtAbsoluteTick);
            log.behaviorError("Plan regeneration failed for villager {}; installed minimal plan floor: dayTime={}",
                    villager.getUUID(), dayTime, exception);
        }
        villager.setDayPlan(newPlan);
        return newPlan;
    }

    private DayPlan hardReset(@Nonnull ServerLevel level,
                              @Nonnull BaseVillager villager,
                              @Nonnull PlanRuntimeState runtime,
                              long dayTime,
                              @Nonnull String reason,
                              @Nonnull DeltaResult delta) {
        DayPlan oldPlan = villager.getDayPlan();
        if (oldPlan != null) {
            oldPlan.getCurrentSlot()
                    .filter(slot -> slot.getStatus() == PlanSlotStatus.ACTIVE)
                    .ifPresent(slot -> slot.markStatus(PlanSlotStatus.INTERRUPTED));
        }

        // Carry forward the old plan's still-future pinned placements before it is discarded, so a
        // reset never silently wipes a fixed-time commitment.
        List<PinnedSelection> carriedPins = oldPlan == null
                ? List.of()
                : collectCarriedPins(oldPlan, nowCivilFor(oldPlan, dayTime));
        PlanIntent intent = carriedPins.isEmpty() ? PlanIntent.empty() : PlanIntent.ofCarriedPins(carriedPins);

        this.forceStop(level, villager);
        runtime.reset(dayTime);
        DayPlan newPlan = this.regeneratePlan(level, villager, dayTime, intent);
        // Recompute against the NEW plan's own calendar day — it can differ from the plan that was
        // just replaced (e.g. a hard reset regenerating into the next authored day).
        runSeekLoop(newPlan, nowCivilFor(newPlan, dayTime));

        this.worldEventEmitter.emitDayPlanInvalidated(villager);

        this.logHardReset(villager, dayTime, reason, delta);
        return newPlan;
    }

    /**
     * Pure projection of a hard reset's carry-forward set: every {@code oldPlan} slot that is both
     * pinned and still in the future relative to {@code nowCivil}.
     */
    @VisibleForTesting
    static List<PinnedSelection> collectCarriedPins(@Nullable DayPlan oldPlan, int nowCivil) {
        if (oldPlan == null) {
            return List.of();
        }
        return oldPlan.getSlots().stream()
                .filter(PlanSlot::isPinned)
                .filter(slot -> slot.getStartTick() > nowCivil)
                .map(slot -> new PinnedSelection(slot.getBehaviorKey(), slot.getStartTick(), false))
                .toList();
    }

    private void logHardReset(@Nonnull BaseVillager villager,
                              long dayTime,
                              @Nonnull String reason,
                              @Nonnull DeltaResult delta) {
        if (RESET_REASON_MISSING_PLAN.equals(reason)
                || RESET_REASON_MISSING_SUCCESSOR.equals(reason)) {
            log.behaviorStatus("Plan hard reset for villager {}: reason={}, dayTime={}, previousDayTime={}, rawDelta={}",
                    villager.getUUID(), reason, dayTime, delta.previousDayTime(), delta.rawDelta());
            return;
        }

        log.behaviorWarn("Plan hard reset for villager {}: reason={}, dayTime={}, previousDayTime={}, rawDelta={}",
                villager.getUUID(), reason, dayTime, delta.previousDayTime(), delta.rawDelta());
    }

    private void drainPendingArrivals(@Nonnull PlanRuntimeState runtime, @Nonnull BaseVillager villager, long dayTime) {
        long currentCalendarDay = WorldCalendar.calendarDayOf(dayTime);
        PlanArrival arrived;
        while ((arrived = runtime.getPendingArrivals().poll()) != null) {
            runtime.setPendingArrival(arbitrate(runtime.getPendingArrival(), arrived, currentCalendarDay));
            runtime.clearPendingFuture();
        }

        CompletableFuture<DayPlan> future = runtime.getPendingFuture();
        if (future != null && future.isCompletedExceptionally() && runtime.getPendingNextPlan() == null) {
            // Failed workers cannot touch villager state; fallback generation runs here on the server thread.
            long wakeAtAbsoluteTick = runtime.getPendingFutureWakeAtAbsoluteTick();
            try {
                DayPlan fallback = this.planGenerator.generate(this.createGenerationContext(villager, wakeAtAbsoluteTick));
                runtime.setPendingArrival(new PlanArrival(fallback, PlanAuthor.HEURISTIC));
                log.behaviorWarn("Async plan generation failed for villager {}; sync fallback installed",
                        villager.getUUID());
            } catch (Exception exception) {
                // Upon exception, build minimal plan
                DayPlan minimalPlan = this.minimalPlanFactory.create(villager, wakeAtAbsoluteTick);
                runtime.setPendingArrival(new PlanArrival(minimalPlan, PlanAuthor.HEURISTIC));
                log.behaviorError("Sync fallback plan generation failed for villager {}; installed minimal plan floor",
                        villager.getUUID(), exception);
            } finally {
                // Clear the failed future either way so this branch does not re-run every tick.
                runtime.clearPendingFuture();
            }
        }
    }

    /**
     * Chooses which of two competing arrivals should be staged, keying on target calendar day then author.
     */
    @VisibleForTesting
    @Nullable
    static PlanArrival arbitrate(@Nullable PlanArrival staged, @Nonnull PlanArrival incoming, long currentCalendarDay) {
        if (incoming.targetDay() < currentCalendarDay) {
            return staged;
        }
        if (staged == null) {
            return incoming;
        }
        if (incoming.targetDay() > staged.targetDay()) {
            return incoming;
        }
        if (incoming.targetDay() < staged.targetDay()) {
            return staged;
        }
        // Same target day: a heuristic arrival must never displace an already-staged LLM overlay.
        if (incoming.author() == PlanAuthor.HEURISTIC && staged.author() == PlanAuthor.LLM) {
            return staged;
        }
        return incoming;
    }

    @Nullable
    private DayPlan adoptPendingIfReady(@Nonnull ServerLevel level,
                                        @Nonnull BaseVillager villager,
                                        @Nonnull PlanRuntimeState runtime,
                                        long dayTime) {
        DayPlan pending = runtime.getPendingNextPlan();
        if (pending == null || dayTime < pending.getWakeAtAbsoluteTick()) {
            return null;
        }

        // Never swap mid-slot
        if (hasActiveCurrentSlot(villager.getDayPlan())) {
            return null;
        }

        // forceStop discharges any lingering teardown obligations
        this.forceStop(level, villager);

        villager.setDayPlan(pending);
        runtime.reset(dayTime);
        log.behaviorStatus("Plan adopted pre-generated plan at scheduled wake for villager {}: wakeAtAbsoluteTick={}",
                villager.getUUID(), pending.getWakeAtAbsoluteTick());
        return pending;
    }

    @VisibleForTesting
    static boolean hasActiveCurrentSlot(@Nullable DayPlan plan) {
        if (plan == null) {
            return false;
        }
        return plan.getCurrentSlot()
                .filter(slot -> slot.getStatus() == PlanSlotStatus.ACTIVE)
                .isPresent();
    }

    private void submitNextPlanAsync(@Nonnull ServerLevel level,
                                     @Nonnull BaseVillager villager,
                                     @Nonnull PlanRuntimeState runtime,
                                     @Nonnull DayPlan currentPlan) {
        if (runtime.getPendingNextPlan() != null || runtime.getPendingFuture() != null) {
            return;
        }

        long dayTime = level.getDayTime();
        long nextWakeAtAbsoluteTick = this.wakeScheduler.nextWakeAtAbsoluteTick(villager, dayTime, currentPlan.getWakeAtAbsoluteTick());
        PlanGenerationContext context = this.createGenerationContext(villager, nextWakeAtAbsoluteTick);
        CompletableFuture<DayPlan> future = this.asyncPlanGenerator.generateAsync(context);
        runtime.setPendingFuture(future);
        runtime.setPendingFutureSubmittedAtDayTime(dayTime);
        runtime.setPendingFutureWakeAtAbsoluteTick(nextWakeAtAbsoluteTick);
        future.whenComplete((plan, error) -> {
            if (error != null) {
                log.behaviorWarn("Async plan generation failed for villager {}: {}", villager.getUUID(), error.toString());
                return;
            }
            runtime.getPendingArrivals().offer(new PlanArrival(plan, PlanAuthor.HEURISTIC));
        });
        log.debug("Submitted async next-plan generation for villager {}: wakeAtAbsoluteTick={}",
                villager.getUUID(), nextWakeAtAbsoluteTick);

        // Forward time-jumps (player sleep, /time set) also reach this method via the
        // calendar-mismatch/overdue branches in tick(), so this single hook covers both normal
        // day-end and time-jumps without a separate catch-up path. A reset/first-spawn villager is
        // deliberately NOT covered here (hardReset/regeneratePlan never calls this method) — it
        // runs the heuristic plan for its current partial day and only picks up an LLM overlay at
        // its next natural exhaustion, avoiding a request storm when a village first loads.
        this.planRequestService.enqueueForOverlay(villager, dayTime, nextWakeAtAbsoluteTick);
    }

    private void completeExpiredPlanAndSubmitSuccessor(@Nonnull ServerLevel level,
                                                       @Nonnull BaseVillager villager,
                                                       @Nonnull PlanRuntimeState runtime,
                                                       @Nonnull DayPlan plan) {
        // Plan-overdue and calendar-mismatch callers reach this mid-run, with a RUNNING behavior
        // still holding the body; forceStop discharges its teardown obligations (nav, held item,
        // sessions) instead of just nulling the reference, and is a no-op when nothing is running.
        this.forceStop(level, villager);
        plan.markStatus(PlanStatus.COMPLETED);
        if (!runtime.isPlanExhausted()) {
            runtime.markPlanExhausted();
            this.worldEventEmitter.emitPlanExhausted(villager);
            this.submitNextPlanAsync(level, villager, runtime, plan);
        }
    }

    private boolean shouldFallbackFromAsyncOverrun(@Nonnull PlanRuntimeState runtime, long dayTime) {
        CompletableFuture<DayPlan> pendingFuture = runtime.getPendingFuture();
        DayPlan pendingNextPlan = runtime.getPendingNextPlan();
        return pendingFuture != null
                && !pendingFuture.isDone()
                && pendingNextPlan == null
                && runtime.isPlanExhausted()
                && dayTime >= runtime.getPendingFutureWakeAtAbsoluteTick();
    }

    @VisibleForTesting
    static boolean shouldWaitForPendingPlan(@Nullable DayPlan pendingNextPlan, long dayTime) {
        return pendingNextPlan != null && dayTime < pendingNextPlan.getWakeAtAbsoluteTick();
    }

    @VisibleForTesting
    static boolean hasCalendarDayMismatch(@Nonnull DayPlan plan, long dayTime) {
        return plan.getCalendarDay() != WorldCalendar.calendarDayOf(dayTime);
    }

    private boolean isPlanOverdue(@Nonnull DayPlan plan, long dayTime) {
        // Backstop for stuck slots or interrupt overflow where natural cursor exhaustion never fires.
        return dayTime >= plan.getWakeAtAbsoluteTick() + plan.getSchedule().authoredDayDurationTicks();
    }

    private long currentWakeAtOrBefore(@Nonnull BaseVillager villager, long dayTime) {
        long currentCalendarDay = WorldCalendar.calendarDayOf(dayTime);
        long todayWake = this.wakeScheduler.wakeAbsoluteTickFor(villager, currentCalendarDay);
        if (todayWake > dayTime) {
            // Today's wake event hasn't happened yet (e.g. world-start before a librarian's 8am, or
            // mid-pre-dawn of a farmer whose 4:30am wake is still upcoming).
            long yesterdayWake = this.wakeScheduler.wakeAbsoluteTickFor(villager, currentCalendarDay - 1);
            return Math.max(0L, yesterdayWake);
        }
        return Math.max(0L, todayWake);
    }

    @VisibleForTesting
    static void runSeekLoop(@Nonnull DayPlan plan, int nowCivil) {
        while (!plan.isExhausted()) {
            Optional<PlanSlot> maybeSlot = plan.getCurrentSlot();
            if (maybeSlot.isEmpty()) {
                return;
            }

            PlanSlot slot = maybeSlot.get();
            switch (slot.getStatus()) {
                case ACTIVE -> {
                    return;
                }
                case PENDING -> {
                    if (!isSlotWindowClosed(slot, nowCivil)) {
                        return;
                    }
                    slot.markStatus(PlanSlotStatus.SKIPPED);
                    plan.advanceSlot();
                }
                case COMPLETED, SKIPPED, INTERRUPTED -> plan.advanceSlot();
            }
        }
    }

    @VisibleForTesting
    static boolean detectOnLoadBackward(@Nonnull DayPlan plan, int nowCivil) {
        int previousSlotIndex = plan.getCurrentSlotIndex() - 1;
        if (previousSlotIndex < 0 || previousSlotIndex >= plan.getSlots().size()) {
            return false;
        }

        PlanSlot lastExecuted = plan.getSlots().get(previousSlotIndex);
        return lastExecuted.getStartTick() > nowCivil;
    }

    @VisibleForTesting
    static boolean isSlotWindowClosed(@Nonnull PlanSlot slot, int nowCivil) {
        return nowCivil > slot.getStartTick() + slot.getEstimatedDurationTicks();
    }

    @VisibleForTesting
    static boolean isSlotWindowOpen(@Nonnull PlanSlot slot, int nowCivil) {
        return nowCivil >= slot.getStartTick();
    }

    private void setPlanActiveMemory(@Nonnull BaseVillager villager) {
        villager.getBrain().setMemory(MemoryTypeRegistry.PLAN_BEHAVIOR_ACTIVE.getModuleType(), true);
    }

    private void clearPlanActiveMemory(@Nonnull BaseVillager villager) {
        villager.getBrain().eraseMemory(MemoryTypeRegistry.PLAN_BEHAVIOR_ACTIVE.getModuleType());
    }

    /**
     * The plan's "extended now": how far {@code dayTime} sits past THIS plan's own calendar-day
     * midnight, deliberately unbounded (see {@link WorldCalendar#civilOffsetWithin}) rather than
     * folded back into {@code [0, TICKS_PER_DAY)} — a value past bedtime must read as "after
     * everything" (closing every remaining slot window), not wrap around to look like early morning.
     */
    private static int nowCivilFor(@Nonnull DayPlan plan, long dayTime) {
        return WorldCalendar.civilOffsetWithin(plan.getCalendarDay(), dayTime);
    }

}
