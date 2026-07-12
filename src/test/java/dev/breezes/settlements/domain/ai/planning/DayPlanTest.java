package dev.breezes.settlements.domain.ai.planning;

import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DayPlanTest {

    @Test
    void builder_populatesFields() {
        PlanSlot slot = slot(BehaviorKey.EAT_FOOD, 1_000);

        DayPlan plan = DayPlan.builder()
                .slot(slot)
                .dayType(PlanDayType.WORK_DAY)
                .calendarDay(2L)
                .schedule(schedule(4_500))
                .build();

        assertEquals(List.of(slot), plan.getSlots());
        assertEquals(PlanStatus.PENDING, plan.getStatus());
        assertEquals(PlanDayType.WORK_DAY, plan.getDayType());
        assertEquals(2L, plan.getCalendarDay());
        // Derived from calendarDay + the schedule's civil wake tick: 2*24000 - 6000 + 4500 = 46500.
        assertEquals(46_500L, plan.getWakeAtAbsoluteTick());
        assertEquals(0, plan.getCurrentSlotIndex());
    }

    @Test
    void schedule_authoredDayDurationTicksIsPlainDifference() {
        // Arrange — civil space never wraps, so duration is a plain subtraction.
        DayPlanSchedule schedule = DayPlanSchedule.builder()
                .wakeTick(4_500)
                .bedtimeTick(18_209)
                .build();

        // Act
        int duration = schedule.authoredDayDurationTicks();

        // Assert
        assertEquals(13_709, duration);
    }

    @Test
    void schedule_rejectsBedtimeAtOrBeforeWakeTick() {
        // Arrange, Act, Assert — a schedule crossing (or exactly touching) midnight is illegal in
        // civil space; every real profile wakes and sleeps within the same civil day.
        assertThrows(IllegalArgumentException.class, () -> DayPlanSchedule.builder()
                .wakeTick(12_000)
                .bedtimeTick(6_000)
                .build());
    }

    @Test
    void builder_ordersSlotsChronologically() {
        PlanSlot late = slot(BehaviorKey.TRADE_ACCEPT, 3_000);
        PlanSlot early = slot(BehaviorKey.EAT_FOOD, 1_000);
        PlanSlot middle = slot(BehaviorKey.TRADE_INITIATE, 2_000);

        DayPlan plan = DayPlan.builder()
                .slot(late)
                .slot(early)
                .slot(middle)
                .dayType(PlanDayType.WORK_DAY)
                .calendarDay(1L)
                .schedule(schedule())
                .build();

        assertEquals(List.of(early, middle, late), plan.getSlots());
        assertEquals(early, plan.getCurrentSlot().orElseThrow());
    }

    @Test
    void builder_ordersSlotsChronologicallyByPlainCivilTick() {
        // Civil ticks never wrap, so pre-dawn slots are simply small ints that sort naturally —
        // no epoch-relative wrap math needed, unlike the old Minecraft-tick representation.
        PlanSlot earlyMorning = slot(BehaviorKey.EAT_FOOD, 4_167);      // ~04:10 civil
        PlanSlot postDawn = slot(BehaviorKey.TRADE_INITIATE, 6_500);    // 06:30 civil
        PlanSlot midDay = slot(BehaviorKey.TRADE_ACCEPT, 12_000);       // 12:00 civil

        DayPlan plan = DayPlan.builder()
                .slot(midDay)
                .slot(postDawn)
                .slot(earlyMorning)
                .dayType(PlanDayType.WORK_DAY)
                .calendarDay(1L)
                .schedule(schedule(4_000))
                .build();

        assertEquals(List.of(earlyMorning, postDawn, midDay), plan.getSlots());
        assertEquals(earlyMorning, plan.getCurrentSlot().orElseThrow());
    }

    @Test
    void getCurrentSlot_returnsFirstSlotWhenIndexIsZero() {
        PlanSlot first = slot(BehaviorKey.EAT_FOOD, 1_000);
        DayPlan plan = DayPlan.builder()
                .slot(first)
                .slot(slot(BehaviorKey.TRADE_INITIATE, 2_000))
                .dayType(PlanDayType.WORK_DAY)
                .calendarDay(1L)
                .schedule(schedule())
                .build();

        assertEquals(first, plan.getCurrentSlot().orElseThrow());
    }

    @Test
    void getCurrentSlot_returnsEmptyWhenIndexPastEnd() {
        DayPlan plan = DayPlan.builder()
                .slot(slot(BehaviorKey.EAT_FOOD, 1_000))
                .dayType(PlanDayType.WORK_DAY)
                .calendarDay(1L)
                .schedule(schedule())
                .build();

        plan.advanceSlot();

        assertTrue(plan.getCurrentSlot().isEmpty());
    }

    @Test
    void advanceSlot_incrementsIndex() {
        DayPlan plan = DayPlan.builder()
                .slot(slot(BehaviorKey.EAT_FOOD, 1_000))
                .dayType(PlanDayType.WORK_DAY)
                .calendarDay(1L)
                .schedule(schedule())
                .build();

        plan.advanceSlot();

        assertEquals(1, plan.getCurrentSlotIndex());
    }

    @Test
    void isExhausted_returnsTrueWhenIndexEqualsSlotCount() {
        DayPlan plan = DayPlan.builder()
                .slot(slot(BehaviorKey.EAT_FOOD, 1_000))
                .dayType(PlanDayType.WORK_DAY)
                .calendarDay(1L)
                .schedule(schedule())
                .build();

        assertFalse(plan.isExhausted());
        plan.advanceSlot();

        assertTrue(plan.isExhausted());
    }

    @Test
    void getRemainingSlots_returnsCorrectSublist() {
        PlanSlot first = slot(BehaviorKey.EAT_FOOD, 1_000);
        PlanSlot second = slot(BehaviorKey.TRADE_INITIATE, 2_000);
        PlanSlot third = slot(BehaviorKey.TRADE_ACCEPT, 3_000);
        DayPlan plan = DayPlan.builder()
                .slot(first)
                .slot(second)
                .slot(third)
                .dayType(PlanDayType.WORK_DAY)
                .calendarDay(1L)
                .schedule(schedule())
                .build();

        plan.advanceSlot();

        assertEquals(List.of(second, third), plan.getRemainingSlots());
    }

    private static PlanSlot slot(BehaviorKey behaviorKey, int startTick) {
        return PlanSlot.builder()
                .startTick(startTick)
                .behaviorKey(behaviorKey)
                .priority(1)
                .flexible(true)
                .estimatedDurationTicks(600)
                .build();
    }

    private static DayPlanSchedule schedule() {
        return schedule(0);
    }

    private static DayPlanSchedule schedule(int wakeTick) {
        return DayPlanSchedule.builder()
                .wakeTick(wakeTick)
                .bedtimeTick(12_000)
                .activityBlock(DayPlanActivityBlock.builder()
                        .context(DayPlanActivityContext.IDLE)
                        .startTick(wakeTick)
                        .endTick(12_000)
                        .build())
                .build();
    }

}
