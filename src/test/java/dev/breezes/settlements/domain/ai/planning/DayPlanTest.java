package dev.breezes.settlements.domain.ai.planning;

import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DayPlanTest {

    @Test
    void builder_populatesFields() {
        PlanSlot slot = slot(BehaviorKey.EAT_FOOD, 1_000);

        DayPlan plan = DayPlan.builder()
                .slot(slot)
                .dayType(PlanDayType.WORK_DAY)
                .wakeAtAbsoluteTick(49_000L)
                .schedule(schedule())
                .build();

        assertEquals(List.of(slot), plan.getSlots());
        assertEquals(PlanStatus.PENDING, plan.getStatus());
        assertEquals(PlanDayType.WORK_DAY, plan.getDayType());
        assertEquals(49_000L, plan.getWakeAtAbsoluteTick());
        assertEquals(2L, plan.getCalendarDay());
        assertEquals(0, plan.getCurrentSlotIndex());
    }

    @Test
    void schedule_returnsAuthoredDayDurationAcrossMidnight() {
        // Arrange
        DayPlanSchedule schedule = DayPlanSchedule.builder()
                .wakeTick(23_000)
                .bedtimeTick(12_000)
                .build();

        // Act
        int duration = schedule.authoredDayDurationTicks();

        // Assert
        assertEquals(13_000, duration);
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
                .wakeAtAbsoluteTick(1L)
                .schedule(schedule())
                .build();

        assertEquals(List.of(early, middle, late), plan.getSlots());
        assertEquals(early, plan.getCurrentSlot().orElseThrow());
    }

    @Test
    void builder_ordersSlotsChronologicallyAcrossDayBoundary() {
        // Farmer epoch: tick 23 000 = 5am. Slots span the 6am (tick 0) boundary.
        int epoch = 23_000;
        PlanSlot preDawn = slot(BehaviorKey.EAT_FOOD, 23_500);    // 5:30am — before tick 0
        PlanSlot postDawn = slot(BehaviorKey.TRADE_INITIATE, 500); // 6:30am — after tick 0
        PlanSlot midDay = slot(BehaviorKey.TRADE_ACCEPT, 6_000);   // 12pm

        DayPlan plan = DayPlan.builder()
                .slot(midDay)
                .slot(postDawn)
                .slot(preDawn)
                .dayType(PlanDayType.WORK_DAY)
                .wakeAtAbsoluteTick(1L)
                .schedule(schedule(epoch))
                .dayStartTick(epoch)
                .build();

        // Chronological order: preDawn (5:30am) → postDawn (6:30am) → midDay (12pm)
        assertEquals(List.of(preDawn, postDawn, midDay), plan.getSlots());
        assertEquals(preDawn, plan.getCurrentSlot().orElseThrow());
    }

    @Test
    void getCurrentSlot_returnsFirstSlotWhenIndexIsZero() {
        PlanSlot first = slot(BehaviorKey.EAT_FOOD, 1_000);
        DayPlan plan = DayPlan.builder()
                .slot(first)
                .slot(slot(BehaviorKey.TRADE_INITIATE, 2_000))
                .dayType(PlanDayType.WORK_DAY)
                .wakeAtAbsoluteTick(1L)
                .schedule(schedule())
                .build();

        assertEquals(first, plan.getCurrentSlot().orElseThrow());
    }

    @Test
    void getCurrentSlot_returnsEmptyWhenIndexPastEnd() {
        DayPlan plan = DayPlan.builder()
                .slot(slot(BehaviorKey.EAT_FOOD, 1_000))
                .dayType(PlanDayType.WORK_DAY)
                .wakeAtAbsoluteTick(1L)
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
                .wakeAtAbsoluteTick(1L)
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
                .wakeAtAbsoluteTick(1L)
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
                .wakeAtAbsoluteTick(1L)
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
                .reason("test")
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
                        .reason("test")
                        .build())
                .build();
    }

}
