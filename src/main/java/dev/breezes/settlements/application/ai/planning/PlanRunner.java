package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.application.ai.behavior.publication.BehaviorOutcomePublisher;
import dev.breezes.settlements.application.ai.catalog.BehaviorPoolResolver;
import dev.breezes.settlements.application.ai.override.OverridePolicy;
import dev.breezes.settlements.application.ai.override.OverrideRequest;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.behavior.contracts.ConfirmableOverride;
import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.domain.ai.behavior.model.BehaviorStatus;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.catalog.BehaviorPlanningMetadata;
import dev.breezes.settlements.domain.ai.catalog.IBehaviorCatalog;
import dev.breezes.settlements.domain.ai.credibility.ReputationQuery;
import dev.breezes.settlements.domain.ai.knowledge.KnowledgeEntry;
import dev.breezes.settlements.domain.ai.knowledge.KnowledgeResolution;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.ai.planning.IAsyncPlanGenerator;
import dev.breezes.settlements.domain.ai.planning.IPlanGenerator;
import dev.breezes.settlements.domain.ai.planning.IWakeTickResolver;
import dev.breezes.settlements.domain.ai.planning.PlanGenerationContext;
import dev.breezes.settlements.domain.ai.planning.PlanSlot;
import dev.breezes.settlements.domain.ai.planning.PlanSlotStatus;
import dev.breezes.settlements.domain.ai.planning.PlanStatus;
import dev.breezes.settlements.domain.ai.schedule.IWeekCycleProvider;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import dev.breezes.settlements.domain.ai.schedule.RestDayPolicy;
import dev.breezes.settlements.domain.ai.schedule.ScheduleProfile;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventEmitter;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
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

import static dev.breezes.settlements.domain.time.TimeOfDay.TICKS_PER_DAY;

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
    private static final String RESET_REASON_INVESTIGATE_CONFIRMED = "investigate confirmed tip";

    private static final int OVERRIDE_TICK_DELTA = 1;

    private static final Comparator<OverridePolicy> OVERRIDE_POLICY_PRECEDENCE = Comparator
            .comparingInt(OverridePolicy::priority)
            .reversed()
            .thenComparing(policy -> policy.getClass().getName());

    private final IBehaviorCatalog catalog;
    private final BehaviorPoolResolver behaviorPoolResolver;
    private final IPlanGenerator planGenerator;
    private final IAsyncPlanGenerator asyncPlanGenerator;
    private final IWeekCycleProvider weekCycleProvider;
    private final IWakeTickResolver wakeTickResolver;
    private final Set<OverridePolicy> overridePolicies;
    private final BehaviorOutcomePublisher behaviorOutcomePublisher;
    private final WorldEventEmitter worldEventEmitter;
    private final ReputationQuery reputationQuery;

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
        int dayTick = currentDayTick(dayTime);
        DeltaResult delta = runtime.advanceClock(dayTime);

        this.drainPendingArrivals(runtime, villager);
        DayPlan adopted = this.adoptPendingIfReady(villager, runtime, dayTime);
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
        } else if (delta.firstTick() && detectOnLoadBackward(plan, dayTick, plan.getDayStartTick())) {
            // Runtime clocks are transient across reloads; persisted slot order is the only reliable signal for unloaded time travel.
            plan = this.hardReset(level, villager, runtime, dayTime, RESET_REASON_BACKWARD_WHILE_UNLOADED, delta);
        } else if (delta.deltaTicks() == 0) {
            // Frozen daylight should freeze planned behavior too, otherwise villagers drift through schedules while the world clock is paused.
            return;
        } else {
            // Forward jumps should catch up pending schedule windows without interrupting an active behavior mid-execution.
            runSeekLoop(plan, dayTick, plan.getDayStartTick());
        }

        if (plan.isExhausted() || plan.getCurrentSlot().isEmpty()) {
            this.completeExpiredPlanAndSubmitSuccessor(level, villager, runtime, plan);
            return;
        }

        this.tickSlot(level, villager, plan, plan.getCurrentSlot().get(), runtime,
                delta.deltaTicks(), dayTick, plan.getDayStartTick());
    }

    public void ensureValidPlan(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        PlanRuntimeState runtime = villager.getPlanRuntimeState();
        long dayTime = level.getDayTime();
        this.drainPendingArrivals(runtime, villager);
        DayPlan plan = this.adoptPendingIfReady(villager, runtime, dayTime);
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
            long nextWakeAtAbsoluteTick = this.nextWakeAtAbsoluteTick(villager, dayTime, plan.getWakeAtAbsoluteTick());
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
                          int dayTick,
                          int planEpoch) {
        switch (slot.getStatus()) {
            case PENDING -> this.tryStartSlot(level, villager, plan, slot, runtime, delta, dayTick, planEpoch);
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
                              int dayTick,
                              int planEpoch) {
        if (!isSlotWindowOpen(slot, dayTick, planEpoch)) {
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
            }
            return;
        }

        behavior.start(level, villager);
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

        behavior.tick(delta, level, villager);
        if (behavior.getStatus() == BehaviorStatus.STOPPED) {
            slot.markStatus(PlanSlotStatus.COMPLETED);
            this.behaviorOutcomePublisher.publishCompleted(villager, slot.getBehaviorKey(), behavior);
            runtime.clearCurrentBehavior();
            this.clearPlanActiveMemory(villager);
            plan.advanceSlot();
        }
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

        override.tick(OVERRIDE_TICK_DELTA, level, villager);

        if (override.getStatus() == BehaviorStatus.STOPPED) {
            this.onOverrideCompleted(level, villager, runtime, runtime.getOverrideBehaviorKey(), override);
        }
    }

    /**
     * TODO:CONFIRM -- if we used llm-based plan gen here, which takes some period of time to complete, this could be bad
     * Handles override completion. A confirmed Investigate override regenerates the day
     * plan so the freshly verified resource can be acted on — the interrupted plan was authored
     * before the tip was known, so blindly resuming it would ignore the discovery (the roadmap's
     * "onConfirm follow-up"). Every other override — and a refuted or unreachable Investigate —
     * simply re-queues the interrupted slot, which is the existing Phase 2 resume behavior.
     */
    private void onOverrideCompleted(@Nonnull ServerLevel level,
                                     @Nonnull BaseVillager villager,
                                     @Nonnull PlanRuntimeState runtime,
                                     @Nullable BehaviorKey completedKey,
                                     @Nonnull IBehavior<BaseVillager> completed) {
        // Behaviors that implement ConfirmableOverride signal when their outcome warrants
        // a plan regeneration. Any confirmed resource discovery should update the plan so
        // the freshly verified fact can influence what happens next.
        boolean shouldRegeneratePlan = completed instanceof ConfirmableOverride confirmable
                && confirmable.didConfirm();
        if (completedKey != null) {
            this.behaviorOutcomePublisher.publishCompleted(villager, completedKey, completed);
        } else {
            log.behaviorWarn("Override completed without behavior key for villager {}", villager.getUUID());
        }
        runtime.clearOverride();

        if (shouldRegeneratePlan) {
            log.behaviorStatus("Override '{}' confirmed for villager {}; regenerating plan", completedKey, villager.getUUID());
            this.hardReset(level, villager, runtime, level.getDayTime(),
                    RESET_REASON_INVESTIGATE_CONFIRMED, singleTickDelta(runtime));
            return;
        }

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

        behavior.start(level, villager);
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

    private DayPlan generatePlan(@Nonnull ServerLevel level, @Nonnull BaseVillager villager, long wakeAtAbsoluteTick) {
        return this.planGenerator.generate(this.createGenerationContext(villager, wakeAtAbsoluteTick));
    }

    private PlanGenerationContext createGenerationContext(@Nonnull BaseVillager villager, long wakeAtAbsoluteTick) {
        VillagerProfessionKey professionKey = villager.getProfession();
        PlanDayType dayType = this.weekCycleProvider.getDayType(WorldCalendar.calendarDayOf(wakeAtAbsoluteTick));

        // Count unverified hearsay tips so the planner can inject Investigate scout slots.
        int pendingTipCount = 0;
        for (KnowledgeEntry entry : villager.getKnowledgeStore().entriesView()) {
            if (entry.isHearsay() && entry.getResolution() == KnowledgeResolution.UNRESOLVED) {
                pendingTipCount++;
            }
        }

        return PlanGenerationContext.builder()
                .profession(professionKey)
                .genetics(villager.getGenetics().copy())
                .scheduleProfile(ScheduleProfile.defaultFor(professionKey))
                .restDayPolicy(RestDayPolicy.defaultFor(professionKey))
                .dayType(dayType)
                .availableBehaviors(this.behaviorPoolResolver.resolve(professionKey))
                .wakeAtAbsoluteTick(wakeAtAbsoluteTick)
                .chronotypeSeed(chronotypeSeedFor(villager))
                .pendingInvestigateTipCount(pendingTipCount)
                .build();
    }

    private DayPlan regeneratePlan(@Nonnull ServerLevel level, @Nonnull BaseVillager villager, long dayTime) {
        long wakeAtAbsoluteTick = currentWakeAtOrBefore(villager, dayTime);
        DayPlan newPlan = this.generatePlan(level, villager, wakeAtAbsoluteTick);
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

        this.forceStop(level, villager);
        runtime.reset(dayTime);
        DayPlan newPlan = this.regeneratePlan(level, villager, dayTime);
        runSeekLoop(newPlan, currentDayTick(dayTime), newPlan.getDayStartTick());

        this.worldEventEmitter.emitDayPlanInvalidated(villager);

        this.logHardReset(villager, dayTime, reason, delta);
        return newPlan;
    }

    private void logHardReset(@Nonnull BaseVillager villager,
                              long dayTime,
                              @Nonnull String reason,
                              @Nonnull DeltaResult delta) {
        if (RESET_REASON_MISSING_PLAN.equals(reason)
                || RESET_REASON_MISSING_SUCCESSOR.equals(reason)
                || RESET_REASON_INVESTIGATE_CONFIRMED.equals(reason)) {
            log.behaviorStatus("Plan hard reset for villager {}: reason={}, dayTime={}, previousDayTime={}, rawDelta={}",
                    villager.getUUID(), reason, dayTime, delta.previousDayTime(), delta.rawDelta());
            return;
        }

        log.behaviorWarn("Plan hard reset for villager {}: reason={}, dayTime={}, previousDayTime={}, rawDelta={}",
                villager.getUUID(), reason, dayTime, delta.previousDayTime(), delta.rawDelta());
    }

    private void drainPendingArrivals(@Nonnull PlanRuntimeState runtime, @Nonnull BaseVillager villager) {
        DayPlan arrived;
        while ((arrived = runtime.getPendingArrivals().poll()) != null) {
            runtime.setPendingNextPlan(arrived);
            runtime.clearPendingFuture();
        }

        CompletableFuture<DayPlan> future = runtime.getPendingFuture();
        if (future != null && future.isCompletedExceptionally() && runtime.getPendingNextPlan() == null) {
            // Failed workers cannot touch villager state; fallback generation runs here on the server thread.
            long wakeAtAbsoluteTick = runtime.getPendingFutureWakeAtAbsoluteTick();
            DayPlan fallback = this.planGenerator.generate(this.createGenerationContext(villager, wakeAtAbsoluteTick));
            runtime.setPendingNextPlan(fallback);
            runtime.clearPendingFuture();
            log.behaviorWarn("Async plan generation failed for villager {}; sync fallback installed",
                    villager.getUUID());
        }
    }

    @Nullable
    private DayPlan adoptPendingIfReady(@Nonnull BaseVillager villager, @Nonnull PlanRuntimeState runtime, long dayTime) {
        DayPlan pending = runtime.getPendingNextPlan();
        if (pending == null || dayTime < pending.getWakeAtAbsoluteTick()) {
            return null;
        }

        villager.setDayPlan(pending);
        runtime.reset(dayTime);
        log.behaviorStatus("Plan adopted pre-generated plan at scheduled wake for villager {}: wakeAtAbsoluteTick={}",
                villager.getUUID(), pending.getWakeAtAbsoluteTick());
        return pending;
    }

    private void submitNextPlanAsync(@Nonnull ServerLevel level,
                                     @Nonnull BaseVillager villager,
                                     @Nonnull PlanRuntimeState runtime,
                                     @Nonnull DayPlan currentPlan) {
        if (runtime.getPendingNextPlan() != null || runtime.getPendingFuture() != null) {
            return;
        }

        long dayTime = level.getDayTime();
        long nextWakeAtAbsoluteTick = this.nextWakeAtAbsoluteTick(villager, dayTime, currentPlan.getWakeAtAbsoluteTick());
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
            runtime.getPendingArrivals().offer(plan);
        });
        log.behaviorStatus("Submitted async next-plan generation for villager {}: wakeAtAbsoluteTick={}",
                villager.getUUID(), nextWakeAtAbsoluteTick);
    }

    private void completeExpiredPlanAndSubmitSuccessor(@Nonnull ServerLevel level,
                                                       @Nonnull BaseVillager villager,
                                                       @Nonnull PlanRuntimeState runtime,
                                                       @Nonnull DayPlan plan) {
        plan.markStatus(PlanStatus.COMPLETED);
        this.clearPlanActiveMemory(villager);
        runtime.clearCurrentBehavior();
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
        long todayWake = this.wakeAbsoluteTickFor(villager, currentCalendarDay);
        if (todayWake > dayTime) {
            // Today's wake event hasn't happened yet (e.g. world-start before a librarian's 8am, or
            // mid-pre-dawn of a farmer whose 4:30am wake is still upcoming).
            long yesterdayWake = this.wakeAbsoluteTickFor(villager, currentCalendarDay - 1);
            return Math.max(0L, yesterdayWake);
        }
        return Math.max(0L, todayWake);
    }

    private long nextWakeAtAbsoluteTick(@Nonnull BaseVillager villager, long dayTime, long currentWakeAtAbsoluteTick) {
        long currentCalendarDay = WorldCalendar.calendarDayOf(dayTime);
        long todayWake = this.wakeAbsoluteTickFor(villager, currentCalendarDay);
        if (todayWake > currentWakeAtAbsoluteTick) {
            return todayWake;
        }
        return this.wakeAbsoluteTickFor(villager, currentCalendarDay + 1);
    }

    private long wakeAbsoluteTickFor(@Nonnull BaseVillager villager, long calendarDay) {
        int wakeTickInMcDay = this.wakeTickFor(villager, calendarDay);
        return WorldCalendar.absoluteTickFor(calendarDay, wakeTickInMcDay);
    }

    private int wakeTickFor(@Nonnull BaseVillager villager, long calendarDay) {
        VillagerProfessionKey professionKey = villager.getProfession();
        ScheduleProfile scheduleProfile = ScheduleProfile.defaultFor(professionKey);
        PlanDayType dayType = this.weekCycleProvider.getDayType(calendarDay);
        return this.wakeTickResolver.resolveWakeTick(scheduleProfile, dayType, chronotypeSeedFor(villager));
    }

    /**
     * Derives a stable, per-villager seed from the UUID so chronotype offsets are reproducible across
     * all call sites that need the same wake tick (scheduling and plan adoption must agree).
     */
    private static long chronotypeSeedFor(@Nonnull BaseVillager villager) {
        return villager.getUUID().getMostSignificantBits() ^ villager.getUUID().getLeastSignificantBits();
    }

    @VisibleForTesting
    static void runSeekLoop(@Nonnull DayPlan plan, int dayTick, int planEpoch) {
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
                    if (!isSlotWindowClosed(slot, dayTick, planEpoch)) {
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
    static boolean detectOnLoadBackward(@Nonnull DayPlan plan, int dayTick, int planEpoch) {
        int previousSlotIndex = plan.getCurrentSlotIndex() - 1;
        if (previousSlotIndex < 0 || previousSlotIndex >= plan.getSlots().size()) {
            return false;
        }

        PlanSlot lastExecuted = plan.getSlots().get(previousSlotIndex);
        int linearLast = Math.floorMod(lastExecuted.getStartTick() - planEpoch, TICKS_PER_DAY);
        int linearNow = Math.floorMod(dayTick - planEpoch, TICKS_PER_DAY);
        return linearLast > linearNow;
    }

    @VisibleForTesting
    static boolean isSlotWindowClosed(@Nonnull PlanSlot slot, int dayTick, int planEpoch) {
        int linearNow = Math.floorMod(dayTick - planEpoch, TICKS_PER_DAY);
        int linearEnd = Math.floorMod(slot.getStartTick() + slot.getEstimatedDurationTicks() - planEpoch, TICKS_PER_DAY);
        return linearNow > linearEnd;
    }

    @VisibleForTesting
    static boolean isSlotWindowOpen(@Nonnull PlanSlot slot, int dayTick, int planEpoch) {
        int linearNow = Math.floorMod(dayTick - planEpoch, TICKS_PER_DAY);
        int linearSlot = Math.floorMod(slot.getStartTick() - planEpoch, TICKS_PER_DAY);
        return linearNow >= linearSlot;
    }

    private void setPlanActiveMemory(@Nonnull BaseVillager villager) {
        villager.getBrain().setMemory(MemoryTypeRegistry.PLAN_BEHAVIOR_ACTIVE.getModuleType(), true);
    }

    private void clearPlanActiveMemory(@Nonnull BaseVillager villager) {
        villager.getBrain().eraseMemory(MemoryTypeRegistry.PLAN_BEHAVIOR_ACTIVE.getModuleType());
    }

    @VisibleForTesting
    static int currentDayTick(long dayTime) {
        return Math.floorMod(dayTime, TICKS_PER_DAY);
    }

}
