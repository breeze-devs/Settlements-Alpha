package dev.breezes.settlements.application.ai.brain;

import dev.breezes.settlements.application.ai.planning.PlanRuntimeState;
import dev.breezes.settlements.bootstrap.registry.activities.ActivityRegistry;
import dev.breezes.settlements.domain.ai.catalog.BehaviorPlanningMetadata;
import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.ai.planning.DayPlanActivityBlock;
import dev.breezes.settlements.domain.ai.planning.DayPlanActivityContext;
import dev.breezes.settlements.domain.ai.planning.DayPlanSchedule;
import dev.breezes.settlements.domain.ai.planning.PlanSlot;
import dev.breezes.settlements.domain.ai.planning.PlanSlotStatus;
import dev.breezes.settlements.domain.ai.schedule.ScheduleProfile;
import dev.breezes.settlements.domain.ai.threat.ThreatVerdict;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.time.CivilTime;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.Tickable;
import dev.breezes.settlements.domain.time.TimeOfDay;
import dev.breezes.settlements.domain.world.WorldCalendar;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.schedule.Activity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;

/**
 * Derives one villager's non-core activity from its current state, so reactive activities need no separate exit step.
 */
public final class ActivityArbiter {

    private static final Set<Activity> REACTIVE_ACTIVITIES = Set.of(
            ActivityRegistry.COMBAT,
            Activity.PANIC,
            Activity.PRE_RAID,
            ActivityRegistry.RAID_HIDE,
            ActivityRegistry.RAID_CELEBRATE,
            Activity.HIDE);

    private static final ClockTicks EVALUATION_INTERVAL = ClockTicks.seconds(1);

    /**
     * Limits how long a bell can keep HIDE eligible, even if no hiding place is found.
     * Vanilla's hiding timeout is 15 seconds.
     */
    private static final ClockTicks BELL_HIDE_DURATION = ClockTicks.seconds(15);

    private final BaseVillager villager;
    private final Tickable cadence;

    @Nullable
    private ThreatVerdict lastEvaluatedVerdict;

    /**
     * The active non-core activity right after the last evaluation; null before the first evaluation.
     */
    @Nullable
    private Activity activityAfterLastEvaluation;

    ActivityArbiter(@Nonnull BaseVillager villager) {
        this.villager = villager;
        this.cadence = Tickable.staggered(EVALUATION_INTERVAL);
    }

    /**
     * Whether the activity responds to something (danger, a raid, or a bell).
     */
    public static boolean isReactive(@Nonnull Activity activity) {
        return REACTIVE_ACTIVITIES.contains(activity);
    }

    /**
     * Re-derives and asserts the villager's activity if needed.
     * Must be called once per server tick.
     *
     * @param verdict the villager's current threat verdict
     */
    void tick(@Nonnull ThreatVerdict verdict) {
        Brain<Villager> brain = this.villager.getBrain();

        boolean cadenceElapsed = this.cadence.tickCheckAndReset(1);
        boolean verdictChanged = verdict != this.lastEvaluatedVerdict;
        boolean changedByAnotherWriter = this.activityAfterLastEvaluation == null
                || !brain.isActive(this.activityAfterLastEvaluation);
        if (!cadenceElapsed && !verdictChanged && !changedByAnotherWriter) {
            return;
        }

        ServerLevel level = (ServerLevel) this.villager.level();
        Activity desired = this.determineDesiredActivity(level, verdict);
        if (!brain.isActive(desired)) {
            if (desired == Activity.PANIC || desired == ActivityRegistry.COMBAT) {
                // Clear the previous activity's targets so the villager can choose where to navigate to
                brain.eraseMemory(MemoryModuleType.PATH);
                brain.eraseMemory(MemoryModuleType.WALK_TARGET);
                brain.eraseMemory(MemoryModuleType.LOOK_TARGET);

                brain.eraseMemory(MemoryModuleType.BREED_TARGET);
                brain.eraseMemory(MemoryModuleType.INTERACTION_TARGET);
            }

            brain.setActiveActivityIfPossible(desired);
        }

        this.lastEvaluatedVerdict = verdict;
        // Record the activity actually selected so an unmet request is not mistaken for another writer's change on every tick
        this.activityAfterLastEvaluation = brain.getActiveNonCoreActivity().orElse(null);
    }

    private Activity determineDesiredActivity(@Nonnull ServerLevel level, @Nonnull ThreatVerdict verdict) {
        // Ahead of every raid row, so a fighting villager is never sent to shelter by a live wave
        if (verdict == ThreatVerdict.COMBAT) {
            return ActivityRegistry.COMBAT;
        }

        Activity raidActivity = determineRaidActivity(level.getRaidAt(this.villager.blockPosition()));
        if (verdict == ThreatVerdict.PANIC) {
            if (raidActivity == ActivityRegistry.RAID_HIDE) {
                // A panicking villager during a raid hides
                return ActivityRegistry.RAID_HIDE;
            }

            // Non-raid panicking villagers flee
            return Activity.PANIC;
        }

        // The raid outranks the bell because HIDE ends on its own timer, which must not end shelter partway through a wave
        if (raidActivity != null) {
            return raidActivity;
        }

        boolean heardBellRecently = this.villager.getBrain().getMemory(MemoryModuleType.HEARD_BELL_TIME)
                .filter(bellTime -> level.getGameTime() < bellTime + BELL_HIDE_DURATION.getTicks())
                .isPresent();
        if (heardBellRecently) {
            return Activity.HIDE;
        }

        return this.villager.isBaby() ? this.determineBabyDailyActivity(level) : this.determineAdultDailyActivity(level);
    }

    /**
     * The activity the supplied raid calls for, or null when the raid is absent or requires no response.
     */
    @Nullable
    private static Activity determineRaidActivity(@Nullable Raid raid) {
        if (raid == null || raid.isStopped() || raid.isLoss()) {
            return null;
        }
        // Victory is checked before the wave rules: a won raid with no raiders left does not count as between waves,
        // so the wave rules alone would read it as a live wave
        if (raid.isVictory()) {
            return ActivityRegistry.RAID_CELEBRATE;
        }
        if (raid.hasFirstWaveSpawned() && !raid.isBetweenWaves()) {
            return ActivityRegistry.RAID_HIDE;
        }
        return Activity.PRE_RAID;
    }

    private Activity determineBabyDailyActivity(@Nonnull ServerLevel level) {
        int timeOfDay = Math.floorMod(level.getDayTime(), TimeOfDay.TICKS_PER_DAY);
        // TODO: babies don't have day plans, use vanilla logic
        return this.villager.getBrain().getSchedule().getActivityAt(timeOfDay);
    }

    private Activity determineAdultDailyActivity(@Nonnull ServerLevel level) {
        DayPlan plan = this.villager.getDayPlan();
        if (plan == null) {
            // Without a plan, villagers use the default daily rhythm
            return fallbackActivity(this.villager.getProfession(), level.getDayTime());
        }

        DayPlanSchedule schedule = plan.getSchedule();
        int nowCivil = WorldCalendar.civilOffsetWithin(plan.getCalendarDay(), level.getDayTime());
        if (isOutsideAuthoredDay(schedule, nowCivil)) {
            // Final bedtime owns the boundary, so stale foreground slots cannot keep the villager out of REST overnight
            return Activity.REST;
        }

        // Active work or social slots need matching ambient behavior even when the schedule names a different context.
        Optional<Activity> activeSlotActivity = activeSlotActivity(plan, this.villager.getPlanRuntimeState());
        return activeSlotActivity.orElseGet(() -> blockActivity(schedule, nowCivil)
                .orElse(Activity.IDLE));
    }

    private static Optional<Activity> activeSlotActivity(@Nonnull DayPlan currentPlan, @Nonnull PlanRuntimeState runtime) {
        Optional<PlanSlot> currentSlot = currentPlan.getCurrentSlot();
        if (currentSlot.isEmpty() || currentSlot.get().getStatus() != PlanSlotStatus.ACTIVE) {
            return Optional.empty();
        }

        BehaviorPlanningMetadata descriptor = runtime.getCurrentDescriptor();
        if (descriptor == null) {
            return Optional.empty();
        }

        return switch (descriptor.getCategory()) {
            case WORK -> Optional.of(Activity.WORK);
            case SOCIAL -> Optional.of(Activity.MEET);
            case SELF_CARE, LEISURE, COMBAT -> Optional.empty();
        };
    }

    private static Optional<Activity> blockActivity(@Nonnull DayPlanSchedule schedule, int nowCivil) {
        // Heuristic schedules small; switch to binary search if authored schedules grow into many granular blocks
        return schedule.activityBlocks().stream()
                .filter(block -> contains(block, nowCivil))
                .findFirst()
                .map(block -> toMinecraftActivity(block.context()));
    }

    private static boolean contains(@Nonnull DayPlanActivityBlock block, int nowCivil) {
        return nowCivil >= block.startTick() && nowCivil < block.endTick();
    }

    private static boolean isOutsideAuthoredDay(@Nonnull DayPlanSchedule schedule, int nowCivil) {
        // A plan may be evaluated before its wake time; that gap still belongs to sleep.
        return nowCivil >= schedule.bedtimeTick() || nowCivil < schedule.wakeTick();
    }

    private static Activity toMinecraftActivity(@Nonnull DayPlanActivityContext context) {
        return switch (context) {
            case WORK -> Activity.WORK;
            case MEET -> Activity.MEET;
            case IDLE -> Activity.IDLE;
            case REST -> Activity.REST;
        };
    }

    private static Activity fallbackActivity(@Nonnull VillagerProfessionKey profession, long dayTime) {
        ScheduleProfile profile = ScheduleProfile.defaultFor(profession);
        // Midnight-based comparisons keep pre-dawn waking hours before the evening bedtime.
        int wakeCivil = CivilTime.civilFromMcTick(profile.defaultWakeTick());
        int bedtimeCivil = CivilTime.civilFromMcTick(profile.defaultSleepTick());
        int nowCivil = CivilTime.civilFromDayTime(dayTime);

        if (nowCivil < wakeCivil || nowCivil >= bedtimeCivil) {
            return Activity.REST;
        }

        int workStartCivil = CivilTime.civilFromMcTick(profile.workStartTick());
        int workEndCivil = CivilTime.civilFromMcTick(profile.workEndTick());
        if (workStartCivil == workEndCivil) {
            // An empty work interval must not invent work or meeting hours.
            return Activity.IDLE;
        }

        int workStart = Math.min(workStartCivil, bedtimeCivil);
        int workEnd = Math.clamp(workEndCivil, workStart, bedtimeCivil);
        if (nowCivil < workStart) {
            return Activity.IDLE;
        }
        if (nowCivil < workEnd) {
            return Activity.WORK;
        }
        return Activity.MEET;
    }

}
