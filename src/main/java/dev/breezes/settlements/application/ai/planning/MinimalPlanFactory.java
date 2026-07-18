package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.planning.Chronotype;
import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.ai.planning.DayPlanActivityBlock;
import dev.breezes.settlements.domain.ai.planning.DayPlanActivityContext;
import dev.breezes.settlements.domain.ai.planning.DayPlanSchedule;
import dev.breezes.settlements.domain.ai.planning.PlanSlot;
import dev.breezes.settlements.domain.ai.schedule.IWeekCycleProvider;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import dev.breezes.settlements.domain.ai.schedule.ScheduleProfile;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.time.CivilTime;
import dev.breezes.settlements.domain.time.GameTicks;
import dev.breezes.settlements.domain.time.TimeOfDay;
import dev.breezes.settlements.domain.world.WorldCalendar;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.annotations.stylistic.VisibleForTesting;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the terminal-floor {@link DayPlan}: a trivially-valid plan of meals plus a single spanning
 * IDLE block, buildable with ZERO villager brain reads.
 * <p>
 * Used only when normal plan generation throws — converting "generation failed → villager left
 * plan-less / tick loop breaks" into "trivial valid plan + logged error".
 * <p>
 * This fallback consults no behavior pool, opportunity forecast, or economy; its only inputs are the
 * villager's profession, chronotype seed, and the target wake tick, so it can never fail for the
 * reasons the real generator can.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class MinimalPlanFactory {

    /**
     * Minimum civil-tick span the floor forces between wake and bedtime. Comfortably larger than any
     * meal duration, so clamping every meal into {@code [wakeCivil, bedtimeCivil - duration]} can
     * never invert — the floor stays constructible even for a degenerate or out-of-range wake tick.
     */
    private static final int MINIMUM_WAKING_SPAN_TICKS = GameTicks.hours(2).getTicksAsInt();

    private static final int TICKS_PER_DAY = TimeOfDay.TICKS_PER_DAY;

    private final IWeekCycleProvider weekCycleProvider;

    /**
     * Assembles the minimal floor plan for {@code villager}, anchored at {@code wakeAtAbsoluteTick}.
     */
    public DayPlan create(@Nonnull BaseVillager villager, long wakeAtAbsoluteTick) {
        VillagerProfessionKey professionKey = villager.getProfession();

        long calendarDay = WorldCalendar.calendarDayOf(wakeAtAbsoluteTick);
        PlanDayType dayType = this.weekCycleProvider.getDayType(calendarDay);

        long chronotypeSeed = PlanGenerationContextFactory.chronotypeSeedFor(villager);
        return buildMinimalPlan(professionKey, wakeAtAbsoluteTick, dayType, chronotypeSeed);
    }

    @VisibleForTesting
    static DayPlan buildMinimalPlan(@Nonnull VillagerProfessionKey professionKey,
                                    long wakeAtAbsoluteTick,
                                    @Nonnull PlanDayType dayType,
                                    long chronotypeSeed) {
        ScheduleProfile schedule = ScheduleProfile.defaultFor(professionKey);
        Chronotype chronotype = Chronotype.of(chronotypeSeed);

        // The floor's whole contract is that it can never throw, so the frame is hardened into the
        // invariant DayPlan validation assumes (0 < wake < bedtime < TICKS_PER_DAY) rather than
        // trusting the raw wake tick or profile sleep to already satisfy it. Sleep shifts by the
        // same chronotype offset as the composer, so a healthy frame still lines up with the frame a
        // real plan would have used.
        int rawWakeCivil = CivilTime.civilFromDayTime(wakeAtAbsoluteTick);
        int wakeCivil = Math.max(1, Math.min(rawWakeCivil, TICKS_PER_DAY - MINIMUM_WAKING_SPAN_TICKS - 1));
        int rawBedtimeCivil = CivilTime.civilFromMcTick(schedule.defaultSleepTick() + chronotype.wakeSleepOffsetTicks());
        int bedtimeCivil = Math.min(TICKS_PER_DAY - 1, Math.max(rawBedtimeCivil, wakeCivil + MINIMUM_WAKING_SPAN_TICKS));

        List<PlanSlot> slots = new ArrayList<>();
        for (MealAnchorRule rule : DefaultMealAnchorTable.rows()) {
            int placementTick = clampToFrame(rule.placementTick(wakeCivil, chronotype.mealOffsetTicks()),
                    rule.durationTicks(), wakeCivil, bedtimeCivil);
            slots.add(PlanSlot.builder()
                    .startTick(placementTick)
                    .behaviorKey(rule.behaviorKey())
                    .priority(rule.priority())
                    .flexible(false)
                    .estimatedDurationTicks(rule.durationTicks())
                    .build());
        }

        DayPlanSchedule daySchedule = DayPlanSchedule.builder()
                .wakeTick(wakeCivil)
                .bedtimeTick(bedtimeCivil)
                .activityBlock(DayPlanActivityBlock.builder()
                        .context(DayPlanActivityContext.IDLE)
                        .startTick(wakeCivil)
                        .endTick(bedtimeCivil)
                        .build())
                .build();

        return DayPlan.builder()
                .slots(slots)
                .dayType(dayType)
                .calendarDay(WorldCalendar.calendarDayOf(wakeAtAbsoluteTick))
                .schedule(daySchedule)
                .build();
    }

    /**
     * Clamps a meal's start into {@code [wakeCivil, bedtimeCivil - durationTicks]} so the slot always
     * ends within the waking frame (and therefore within the civil day) — the guard that keeps
     * {@link DayPlan}'s slot-window validation from ever rejecting the floor. Safe because
     * {@link #MINIMUM_WAKING_SPAN_TICKS} exceeds any meal duration, so the bounds never invert.
     */
    private static int clampToFrame(int startTick, int durationTicks, int wakeCivil, int bedtimeCivil) {
        int latestStart = bedtimeCivil - durationTicks;
        return Math.max(wakeCivil, Math.min(startTick, latestStart));
    }

}
