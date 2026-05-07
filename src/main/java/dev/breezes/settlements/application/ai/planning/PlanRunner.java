package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.application.ai.catalog.BehaviorPoolResolver;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.domain.ai.behavior.model.BehaviorStatus;
import dev.breezes.settlements.domain.ai.catalog.IBehaviorCatalog;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.ai.planning.IPlanGenerator;
import dev.breezes.settlements.domain.ai.planning.PlanGenerationContext;
import dev.breezes.settlements.domain.ai.planning.PlanSlot;
import dev.breezes.settlements.domain.ai.planning.PlanSlotStatus;
import dev.breezes.settlements.domain.ai.planning.PlanStatus;
import dev.breezes.settlements.domain.ai.schedule.IWeekCycleProvider;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import dev.breezes.settlements.domain.ai.schedule.RestDayPolicy;
import dev.breezes.settlements.domain.ai.schedule.ScheduleProfile;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.annotations.stylistic.VisibleForTesting;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.VillagerProfession;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Optional;

import static dev.breezes.settlements.domain.time.TimeOfDay.TICKS_PER_DAY;

/**
 * Application-layer entry point for executing villager day plans.
 */
@ServerScope
@CustomLog
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class PlanRunner {

    private static final String RESET_REASON_GAME_DAY_ROLLOVER = "gameDay rollover";
    private static final String RESET_REASON_BACKWARD_JUMP = "backward dayTime jump";
    private static final String RESET_REASON_BACKWARD_WHILE_UNLOADED = "backward dayTime jump while unloaded";

    private final IBehaviorCatalog catalog;
    private final BehaviorPoolResolver behaviorPoolResolver;
    private final IPlanGenerator planGenerator;
    private final IWeekCycleProvider weekCycleProvider;

    public void tick(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        PlanRuntimeState runtime = villager.getPlanRuntimeState();
        long dayTime = level.getDayTime();
        int dayTick = currentDayTick(dayTime);
        long gameDay = currentGameDay(dayTime);
        DeltaResult delta = runtime.advanceClock(dayTime);

        DayPlan plan = villager.getDayPlan();
        if (plan == null || plan.getGeneratedForDay() != gameDay) {
            // Plans are scoped to a Minecraft gameDay; carrying old-day intent into a new day desynchronizes meals and work blocks.
            plan = this.hardReset(level, villager, runtime, gameDay, dayTick, dayTime, RESET_REASON_GAME_DAY_ROLLOVER, delta);
        } else if (delta.backwardJump()) {
            // Backward jumps invalidate chronological slot ordering, so retrying the existing cursor would replay future intent in the past.
            plan = this.hardReset(level, villager, runtime, gameDay, dayTick, dayTime, RESET_REASON_BACKWARD_JUMP, delta);
        } else if (delta.firstTick() && detectOnLoadBackward(plan, dayTick, plan.getDayStartTick())) {
            // Runtime clocks are transient across reloads; persisted slot order is the only reliable signal for unloaded time travel.
            plan = this.hardReset(level, villager, runtime, gameDay, dayTick, dayTime, RESET_REASON_BACKWARD_WHILE_UNLOADED, delta);
        } else if (delta.deltaTicks() == 0) {
            // Frozen daylight should freeze planned behavior too, otherwise villagers drift through schedules while the world clock is paused.
            return;
        } else {
            // Forward jumps should catch up pending schedule windows without interrupting an active behavior mid-execution.
            runSeekLoop(plan, dayTick, plan.getDayStartTick());
        }

        if (plan.isExhausted()) {
            plan.markStatus(PlanStatus.COMPLETED);
            this.clearPlanActiveMemory(villager);
            runtime.clearCurrentBehavior();
            return;
        }

        Optional<PlanSlot> currentSlot = plan.getCurrentSlot();
        if (currentSlot.isEmpty()) {
            plan.markStatus(PlanStatus.COMPLETED);
            this.clearPlanActiveMemory(villager);
            runtime.clearCurrentBehavior();
            return;
        }

        this.tickSlot(level, villager, plan, currentSlot.get(), runtime, delta.deltaTicks(), dayTick, plan.getDayStartTick());
    }

    public void ensurePlanForCurrentDay(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        PlanRuntimeState runtime = villager.getPlanRuntimeState();
        long dayTime = level.getDayTime();
        long gameDay = currentGameDay(dayTime);
        DayPlan plan = villager.getDayPlan();
        if (plan != null && plan.getGeneratedForDay() == gameDay) {
            return;
        }

        // REST can span the game-day boundary; prepare the new authored day without linear-time seeking from pre-wake ticks.
        this.forceStop(level, villager);
        runtime.reset(dayTime);
        this.regeneratePlan(level, villager, gameDay);
        log.behaviorStatus("Plan regenerated for villager {} during unmanaged activity: gameDay={}, dayTime={}",
                villager.getUUID(), gameDay, dayTime);
    }

    public void suspendIfActive(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        PlanRuntimeState runtime = villager.getPlanRuntimeState();
        IBehavior<BaseVillager> behavior = runtime.getCurrentBehavior();
        if (behavior == null || behavior.getStatus() == BehaviorStatus.STOPPED) {
            return;
        }

        behavior.requestStop("Plan interrupted");
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
            slot.markStatus(PlanSlotStatus.SKIPPED);
            this.clearPlanActiveMemory(villager);
            plan.advanceSlot();
            return;
        }

        IBehavior<BaseVillager> behavior = maybeBehavior.get();
        behavior.getBehaviorCoolDown().forceComplete();
        behavior.getPreconditionCheckCooldown().forceComplete();

        if (!behavior.tickPreconditions(1, level, villager)) {
            if (slot.isFlexible()) {
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
            runtime.clearCurrentBehavior();
            this.clearPlanActiveMemory(villager);
            plan.advanceSlot();
        }
    }

    private DayPlan generatePlan(@Nonnull ServerLevel level, @Nonnull BaseVillager villager, long gameDay) {
        VillagerProfession profession = villager.getVillagerData().getProfession();
        VillagerProfessionKey professionKey = new VillagerProfessionKey(profession.name());
        PlanDayType dayType = this.weekCycleProvider.getDayType(gameDay);
        return this.planGenerator.generate(PlanGenerationContext.builder()
                .profession(professionKey)
                .genetics(villager.getGenetics())
                .scheduleProfile(ScheduleProfile.defaultFor(professionKey))
                .restDayPolicy(RestDayPolicy.defaultFor(professionKey))
                .dayType(dayType)
                .availableBehaviors(this.behaviorPoolResolver.resolve(professionKey))
                .gameDay(gameDay)
                .build());
    }

    private DayPlan regeneratePlan(@Nonnull ServerLevel level, @Nonnull BaseVillager villager, long gameDay) {
        DayPlan newPlan = this.generatePlan(level, villager, gameDay);
        villager.setDayPlan(newPlan);
        return newPlan;
    }

    private DayPlan hardReset(@Nonnull ServerLevel level,
                              @Nonnull BaseVillager villager,
                              @Nonnull PlanRuntimeState runtime,
                              long gameDay,
                              int dayTick,
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
        DayPlan newPlan = this.regeneratePlan(level, villager, gameDay);
        runSeekLoop(newPlan, dayTick, newPlan.getDayStartTick());

        this.logHardReset(villager, gameDay, dayTime, reason, delta);
        return newPlan;
    }

    private void logHardReset(@Nonnull BaseVillager villager,
                              long gameDay,
                              long dayTime,
                              @Nonnull String reason,
                              @Nonnull DeltaResult delta) {
        if (RESET_REASON_GAME_DAY_ROLLOVER.equals(reason)) {
            log.behaviorStatus("Plan hard reset for villager {}: reason={}, gameDay={}, dayTime={}, previousDayTime={}, rawDelta={}",
                    villager.getUUID(), reason, gameDay, dayTime, delta.previousDayTime(), delta.rawDelta());
            return;
        }

        log.behaviorWarn("Plan hard reset for villager {}: reason={}, gameDay={}, dayTime={}, previousDayTime={}, rawDelta={}",
                villager.getUUID(), reason, gameDay, dayTime, delta.previousDayTime(), delta.rawDelta());
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

    @VisibleForTesting
    static long currentGameDay(long dayTime) {
        return dayTime / TICKS_PER_DAY;
    }

}
