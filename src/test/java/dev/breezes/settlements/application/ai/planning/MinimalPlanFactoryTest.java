package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.ai.planning.PlanSlot;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import dev.breezes.settlements.domain.ai.schedule.ScheduleProfile;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.time.CivilTime;
import dev.breezes.settlements.domain.time.TimeOfDay;
import dev.breezes.settlements.domain.world.WorldCalendar;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Minecraft-free unit tests for the pure floor-plan core
 * {@link MinimalPlanFactory#buildMinimalPlan(VillagerProfessionKey, long, PlanDayType, long)}. All
 * inputs are plain domain values, so no {@code BaseVillager}/brain is involved.
 */
class MinimalPlanFactoryTest {

    /** Small offset spread by {@code Chronotype#mealOffsetTicks()} (±12 game-minutes = ±240 ticks). */
    private static final int MEAL_OFFSET_TOLERANCE_TICKS = 240;

    @Test
    void buildMinimalPlan_workDayFarmer_targetsCalendarDayWithThreeMealSlots() {
        // Arrange — a farmer's real wake tick on calendar day 5.
        long calendarDay = 5L;
        ScheduleProfile schedule = ScheduleProfile.defaultFor(VillagerProfessionKey.FARMER);
        long wakeAtAbsoluteTick = WorldCalendar.absoluteTickFor(calendarDay, schedule.defaultWakeTick());

        // Act
        DayPlan plan = MinimalPlanFactory.buildMinimalPlan(VillagerProfessionKey.FARMER, wakeAtAbsoluteTick,
                PlanDayType.WORK_DAY, 0xABCDEF01L);

        // Assert — identity + meal floor
        assertEquals(calendarDay, plan.getCalendarDay());
        assertEquals(PlanDayType.WORK_DAY, plan.getDayType());

        List<PlanSlot> slots = plan.getSlots();
        assertEquals(3, slots.size(), "the floor plan is exactly the three meal rows");
        assertTrue(slots.stream().allMatch(slot -> slot.getBehaviorKey().equals(BehaviorKey.EAT_FOOD)),
                "every floor slot is a meal");
        assertTrue(slots.stream().noneMatch(PlanSlot::isFlexible), "meals are placed rigid");

        // Breakfast is wake-relative with no chronotype meal offset, so it sits exactly at wake civil.
        int wakeCivil = CivilTime.civilFromDayTime(wakeAtAbsoluteTick);
        assertEquals(wakeCivil, slots.get(0).getStartTick());

        // Lunch/dinner land near their fixed anchors, within the chronotype meal-offset tolerance.
        assertNear(TimeOfDay.AT_11_00.getCivilTick(), slots.get(1).getStartTick());
        assertNear(TimeOfDay.AT_16_30.getCivilTick(), slots.get(2).getStartTick());

        // Every slot ends within its civil day (DayPlan construction would have thrown otherwise).
        assertTrue(slots.stream().allMatch(
                slot -> slot.getStartTick() + slot.getEstimatedDurationTicks() <= TimeOfDay.TICKS_PER_DAY));
    }

    @Test
    void buildMinimalPlan_restDayLibrarian_buildsValidPlanWithLateFrame() {
        // Arrange — a late-rising librarian on a rest day; a different chronotype seed shifts the frame.
        long calendarDay = 8L;
        ScheduleProfile schedule = ScheduleProfile.defaultFor(VillagerProfessionKey.LIBRARIAN);
        long wakeAtAbsoluteTick = WorldCalendar.absoluteTickFor(calendarDay, schedule.defaultWakeTick());

        // Act
        DayPlan plan = MinimalPlanFactory.buildMinimalPlan(VillagerProfessionKey.LIBRARIAN, wakeAtAbsoluteTick,
                PlanDayType.REST_DAY, 12_345L);

        // Assert — construction succeeded, so the frame is valid (0 < wake < bedtime < 24000) and the
        // single spanning IDLE block is well-formed.
        assertEquals(calendarDay, plan.getCalendarDay());
        assertEquals(PlanDayType.REST_DAY, plan.getDayType());
        assertEquals(3, plan.getSlots().size());

        int wakeCivil = plan.getSchedule().wakeTick();
        int bedtimeCivil = plan.getSchedule().bedtimeTick();
        assertTrue(wakeCivil > 0 && wakeCivil < bedtimeCivil && bedtimeCivil < TimeOfDay.TICKS_PER_DAY);

        assertEquals(1, plan.getSchedule().activityBlocks().size());
        assertEquals(wakeCivil, plan.getSchedule().activityBlocks().get(0).startTick());
        assertEquals(bedtimeCivil, plan.getSchedule().activityBlocks().get(0).endTick());
    }

    private static void assertNear(int expected, int actual) {
        assertFalse(Math.abs(expected - actual) > MEAL_OFFSET_TOLERANCE_TICKS,
                "expected %d within %d ticks of %d".formatted(actual, MEAL_OFFSET_TOLERANCE_TICKS, expected));
    }

}
