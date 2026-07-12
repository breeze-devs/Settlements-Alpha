package dev.breezes.settlements.infrastructure.minecraft.behavior.planning;

import dev.breezes.settlements.application.ai.planning.PlanRuntimeState;
import dev.breezes.settlements.domain.ai.catalog.BehaviorPlanningMetadata;
import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.ai.planning.DayPlanActivityBlock;
import dev.breezes.settlements.domain.ai.planning.DayPlanActivityContext;
import dev.breezes.settlements.domain.ai.planning.DayPlanSchedule;
import dev.breezes.settlements.domain.ai.planning.PlanSlot;
import dev.breezes.settlements.domain.ai.planning.PlanSlotStatus;
import dev.breezes.settlements.domain.ai.schedule.ScheduleProfile;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.time.CivilTime;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.ITickable;
import dev.breezes.settlements.domain.world.WorldCalendar;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

public class PlanContextSwitcher extends Behavior<Villager> {

    private static final ClockTicks TICK_COOLDOWN = ClockTicks.seconds(1);

    private final ITickable tickCooldown;

    @Nullable
    private Activity lastDerivedActivity;

    public PlanContextSwitcher() {
        super(Map.of());
        this.tickCooldown = TICK_COOLDOWN.asTickable();
        this.tickCooldown.forceComplete();
        this.lastDerivedActivity = null;
    }

    @Override
    protected boolean timedOut(long gameTime) {
        return false;
    }

    @Override
    protected boolean canStillUse(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        return true;
    }

    @Override
    protected void tick(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        if (!(villager instanceof BaseVillager baseVillager)) {
            return;
        }
        if (!this.tickCooldown.tickCheckAndReset(1)) {
            return;
        }

        Activity targetActivity = this.deriveActivity(level, baseVillager);
        Optional<Activity> activeNonCoreActivity = villager.getBrain().getActiveNonCoreActivity();
        if (targetActivity == this.lastDerivedActivity && activeNonCoreActivity.filter(targetActivity::equals).isPresent()) {
            return;
        }

        // Context only changes at schedule/plan boundaries, so avoid asking Brain to re-assert stable activities every server tick.
        villager.getBrain().setActiveActivityIfPossible(targetActivity);
        this.lastDerivedActivity = targetActivity;
    }

    private Activity deriveActivity(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        Optional<DayPlan> currentPlan = this.currentPlan(level, villager);
        if (currentPlan.isEmpty()) {
            // Missing or stale plans should still give villagers vanilla-shaped ambient life instead of freezing activity transitions.
            return this.fallbackActivity(villager.getProfession(), level.getDayTime());
        }

        DayPlan plan = currentPlan.get();
        DayPlanSchedule schedule = plan.getSchedule();
        int nowCivil = WorldCalendar.civilOffsetWithin(plan.getCalendarDay(), level.getDayTime());
        if (this.isOutsideAuthoredDay(schedule, nowCivil)) {
            // Final bedtime owns the boundary so stale foreground slots cannot keep the villager out of REST overnight.
            return Activity.REST;
        }

        Optional<Activity> activeSlotActivity = this.activeSlotActivity(plan, villager.getPlanRuntimeState());
        return activeSlotActivity.orElseGet(() -> this.blockActivity(schedule, nowCivil)
                .orElse(Activity.IDLE));
    }

    private Optional<DayPlan> currentPlan(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        return Optional.ofNullable(villager.getDayPlan());
    }

    private Optional<Activity> activeSlotActivity(@Nonnull DayPlan currentPlan, @Nonnull PlanRuntimeState runtime) {
        Optional<PlanSlot> currentSlot = currentPlan.getCurrentSlot();
        if (currentSlot.isEmpty() || currentSlot.get().getStatus() != PlanSlotStatus.ACTIVE) {
            return Optional.empty();
        }

        BehaviorPlanningMetadata descriptor = runtime.getCurrentDescriptor();
        if (descriptor == null) {
            return Optional.empty();
        }

        // Active foreground slots get first claim on context so work/social behaviors have matching vanilla affordances.
        return switch (descriptor.getCategory()) {
            case WORK -> Optional.of(Activity.WORK);
            case SOCIAL -> Optional.of(Activity.MEET);
            case SELF_CARE, LEISURE, COMBAT -> Optional.empty();
        };
    }

    private Optional<Activity> blockActivity(@Nonnull DayPlanSchedule schedule, int nowCivil) {
        // Heuristic schedules are intentionally tiny; switch to binary search if authored schedules grow into many granular blocks.
        return schedule.activityBlocks().stream()
                .filter(block -> this.contains(block, nowCivil))
                .findFirst()
                .map(block -> this.toMinecraftActivity(block.context()));
    }

    private boolean contains(@Nonnull DayPlanActivityBlock block, int nowCivil) {
        return nowCivil >= block.startTick() && nowCivil < block.endTick();
    }

    private boolean isOutsideAuthoredDay(@Nonnull DayPlanSchedule schedule, int nowCivil) {
        // The < wake arm matters for a freshly (re)generated plan ticked in the sliver before its
        // own wake civil tick (e.g. right after a hard reset): the OLD wake-relative "linear" check
        // folded a pre-wake "now" forward past bedtime via wraparound, landing on REST; this arm
        // reaches the same REST outcome directly, without relying on any wrap.
        return nowCivil >= schedule.bedtimeTick() || nowCivil < schedule.wakeTick();
    }

    private Activity toMinecraftActivity(@Nonnull DayPlanActivityContext context) {
        return switch (context) {
            case WORK -> Activity.WORK;
            case MEET -> Activity.MEET;
            case IDLE -> Activity.IDLE;
            case REST -> Activity.REST;
        };
    }

    private Activity fallbackActivity(@Nonnull VillagerProfessionKey profession, long dayTime) {
        ScheduleProfile profile = ScheduleProfile.defaultFor(profession);
        // ScheduleProfile ticks are authored in MC space; re-anchor each onto civil time so the
        // comparisons below need no wake-relative wrap math.
        int wakeCivil = CivilTime.civilFromMcTick(profile.defaultWakeTick());
        int bedtimeCivil = CivilTime.civilFromMcTick(profile.defaultSleepTick());
        int nowCivil = CivilTime.civilFromDayTime(dayTime);

        if (nowCivil < wakeCivil || nowCivil >= bedtimeCivil) {
            return Activity.REST;
        }

        int workStartCivil = CivilTime.civilFromMcTick(profile.workStartTick());
        int workEndCivil = CivilTime.civilFromMcTick(profile.workEndTick());
        if (workStartCivil == workEndCivil) {
            // Professions with no work interval, currently Nitwit, should not receive a synthetic work or meet context.
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
