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
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.ITickable;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

import static dev.breezes.settlements.domain.time.TimeOfDay.TICKS_PER_DAY;

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
        int dayTick = Math.floorMod(level.getDayTime(), TICKS_PER_DAY);
        Optional<DayPlan> currentPlan = this.currentPlan(level, villager);
        if (currentPlan.isEmpty()) {
            // Missing or stale plans should still give villagers vanilla-shaped ambient life instead of freezing activity transitions.
            return this.fallbackActivity(villager.getVillagerData().getProfession(), dayTick);
        }

        DayPlanSchedule schedule = currentPlan.get().getSchedule();
        if (this.isOutsideAuthoredDay(schedule, dayTick)) {
            // Final bedtime owns the boundary so stale foreground slots cannot keep the villager out of REST overnight.
            return Activity.REST;
        }

        Optional<Activity> activeSlotActivity = this.activeSlotActivity(currentPlan.get(), villager.getPlanRuntimeState());
        return activeSlotActivity.orElseGet(() -> this.blockActivity(schedule, dayTick)
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

    private Optional<Activity> blockActivity(@Nonnull DayPlanSchedule schedule, int dayTick) {
        int linearNow = this.toLinear(dayTick, schedule.wakeTick());
        // Heuristic schedules are intentionally tiny; switch to binary search if authored schedules grow into many granular blocks.
        return schedule.activityBlocks().stream()
                .filter(block -> this.contains(block, schedule.wakeTick(), linearNow))
                .findFirst()
                .map(block -> this.toMinecraftActivity(block.context()));
    }

    private boolean contains(@Nonnull DayPlanActivityBlock block, int wakeTick, int linearNow) {
        int linearStart = this.toLinear(block.startTick(), wakeTick);
        int linearEnd = this.toLinear(block.endTick(), wakeTick);
        return linearNow >= linearStart && linearNow < linearEnd;
    }

    private boolean isOutsideAuthoredDay(@Nonnull DayPlanSchedule schedule, int dayTick) {
        int linearNow = this.toLinear(dayTick, schedule.wakeTick());
        int linearBedtime = this.toLinear(schedule.bedtimeTick(), schedule.wakeTick());
        return linearNow >= linearBedtime;
    }

    private Activity toMinecraftActivity(@Nonnull DayPlanActivityContext context) {
        return switch (context) {
            case WORK -> Activity.WORK;
            case MEET -> Activity.MEET;
            case IDLE -> Activity.IDLE;
            case REST -> Activity.REST;
        };
    }

    private Activity fallbackActivity(@Nonnull VillagerProfession profession, int dayTick) {
        VillagerProfessionKey professionKey = new VillagerProfessionKey(profession.name());
        ScheduleProfile profile = ScheduleProfile.defaultFor(professionKey);
        // Fallback is computed directly in linear time to avoid constructing wraparound blocks for early-bird professions.
        int bedtimeLinear = this.toLinear(profile.defaultSleepTick(), profile.defaultWakeTick());
        int nowLinear = this.toLinear(dayTick, profile.defaultWakeTick());
        if (nowLinear >= bedtimeLinear) {
            return Activity.REST;
        }

        int workStartLinear = this.toLinear(profile.workStartTick(), profile.defaultWakeTick());
        int workEndLinear = this.toLinear(profile.workEndTick(), profile.defaultWakeTick());
        if (workStartLinear == workEndLinear) {
            // Professions with no work interval, currently Nitwit, should not receive a synthetic work or meet context.
            return Activity.IDLE;
        }

        int workStart = Math.min(workStartLinear, bedtimeLinear);
        int workEnd = Math.clamp(workEndLinear, workStart, bedtimeLinear);
        if (nowLinear < workStart) {
            return Activity.IDLE;
        }
        if (nowLinear < workEnd) {
            return Activity.WORK;
        }
        return Activity.MEET;
    }

    private int toLinear(int tick, int epoch) {
        // Authored days can start before vanilla tick zero, so comparisons must use wake-relative monotonic time.
        return Math.floorMod(tick - epoch, TICKS_PER_DAY);
    }

}
