package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.ai.planning.DayPlanActivityBlock;
import dev.breezes.settlements.domain.ai.planning.DayPlanActivityContext;
import dev.breezes.settlements.domain.ai.planning.DayPlanSchedule;
import dev.breezes.settlements.domain.ai.planning.PlanSlot;
import dev.breezes.settlements.domain.ai.planning.PlanSlotStatus;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import dev.breezes.settlements.domain.time.TimeOfDay;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanRunnerTimingTest {

    @Test
    void isSlotWindowOpen_returnsFalseBeforeStartWindow() {
        // Arrange
        PlanSlot slot = slot(2_000, 600, PlanSlotStatus.PENDING);

        // Act
        boolean open = PlanRunner.isSlotWindowOpen(slot, 1_999);

        // Assert
        assertFalse(open);
    }

    @Test
    void isSlotWindowOpen_returnsTrueAtStartWindow() {
        // Arrange
        PlanSlot slot = slot(2_000, 600, PlanSlotStatus.PENDING);

        // Act
        boolean open = PlanRunner.isSlotWindowOpen(slot, 2_000);

        // Assert
        assertTrue(open);
    }

    @Test
    void isSlotWindowClosed_returnsFalseAtWindowEndBecauseEndIsInclusive() {
        // Arrange
        PlanSlot slot = slot(2_000, 600, PlanSlotStatus.PENDING);

        // Act
        boolean closed = PlanRunner.isSlotWindowClosed(slot, 2_600);

        // Assert
        assertFalse(closed);
    }

    @Test
    void isSlotWindowClosed_returnsTrueAfterWindowEnd() {
        // Arrange
        PlanSlot slot = slot(2_000, 600, PlanSlotStatus.PENDING);

        // Act
        boolean closed = PlanRunner.isSlotWindowClosed(slot, 2_601);

        // Assert
        assertTrue(closed);
    }

    @Test
    void isSlotWindowClosed_extendedPostMidnightNowClosesEveryWindow() {
        // Arrange — nowCivil >= TICKS_PER_DAY means "past this plan's entire authored day"
        // (see WorldCalendar#civilOffsetWithin); every remaining slot window must read as closed.
        PlanSlot slot = slot(23_000, 600, PlanSlotStatus.PENDING);

        // Act
        boolean closed = PlanRunner.isSlotWindowClosed(slot, 25_000);

        // Assert
        assertTrue(closed);
    }

    @Test
    void runSeekLoop_skipsRigidAndFlexiblePendingSlotsWhoseWindowsClosed() {
        // Arrange
        PlanSlot rigidPast = slot(1_000, 600, false, PlanSlotStatus.PENDING);
        PlanSlot flexiblePast = slot(2_000, 600, true, PlanSlotStatus.PENDING);
        PlanSlot current = slot(4_000, 600, true, PlanSlotStatus.PENDING);
        DayPlan plan = plan(rigidPast, flexiblePast, current);

        // Act
        PlanRunner.runSeekLoop(plan, 3_000);

        // Assert
        assertEquals(PlanSlotStatus.SKIPPED, rigidPast.getStatus());
        assertEquals(PlanSlotStatus.SKIPPED, flexiblePast.getStatus());
        assertEquals(PlanSlotStatus.PENDING, current.getStatus());
        assertEquals(2, plan.getCurrentSlotIndex());
    }

    @Test
    void runSeekLoop_stopsAtActiveSlot() {
        // Arrange
        PlanSlot active = slot(1_000, 600, PlanSlotStatus.ACTIVE);
        PlanSlot pending = slot(2_000, 600, PlanSlotStatus.PENDING);
        DayPlan plan = plan(active, pending);

        // Act
        PlanRunner.runSeekLoop(plan, 10_000);

        // Assert
        assertEquals(PlanSlotStatus.ACTIVE, active.getStatus());
        assertEquals(PlanSlotStatus.PENDING, pending.getStatus());
        assertEquals(0, plan.getCurrentSlotIndex());
    }

    @Test
    void runSeekLoop_doesNotSkipPendingSlotsWhenCalledBeforeWake() {
        // Arrange — a negative extended now (see WorldCalendar#civilOffsetWithin) means dayTime is
        // still before this plan's own day began; nothing should be skipped, the plan just waits.
        PlanSlot firstRestDaySlot = slot(1_500, 600, PlanSlotStatus.PENDING);
        DayPlan plan = DayPlan.builder()
                .slot(firstRestDaySlot)
                .dayType(PlanDayType.REST_DAY)
                .calendarDay(1L)
                .schedule(schedule(1_000))
                .build();

        // Act
        PlanRunner.runSeekLoop(plan, -500);

        // Assert
        assertEquals(PlanSlotStatus.PENDING, firstRestDaySlot.getStatus());
        assertEquals(0, plan.getCurrentSlotIndex());
    }

    @Test
    void dayPlanBuilder_rejectsSlotWindowThatCrossesPlanDayBoundary() {
        // Arrange, Act, Assert
        assertThrows(IllegalArgumentException.class, () -> DayPlan.builder()
                .slot(slot(23_700, 500, PlanSlotStatus.PENDING))
                .dayType(PlanDayType.WORK_DAY)
                .calendarDay(1L)
                .schedule(schedule())
                .build());
    }

    @Test
    void shouldWaitForPendingPlan_returnsTrueBeforePendingWake() {
        // Arrange
        DayPlan pendingPlan = DayPlan.builder()
                .dayType(PlanDayType.WORK_DAY)
                .calendarDay(4L)
                .schedule(schedule())
                .build();

        // Act
        boolean shouldWait = PlanRunner.shouldWaitForPendingPlan(pendingPlan, pendingPlan.getWakeAtAbsoluteTick() - 835L);

        // Assert
        assertTrue(shouldWait);
    }

    @Test
    void shouldWaitForPendingPlan_returnsFalseWhenPendingWakeArrived() {
        // Arrange
        DayPlan pendingPlan = DayPlan.builder()
                .dayType(PlanDayType.WORK_DAY)
                .calendarDay(4L)
                .schedule(schedule())
                .build();

        // Act
        boolean shouldWait = PlanRunner.shouldWaitForPendingPlan(pendingPlan, pendingPlan.getWakeAtAbsoluteTick());

        // Assert
        assertFalse(shouldWait);
    }

    @Test
    void shouldWaitForPendingPlan_returnsFalseWithoutPendingPlan() {
        // Arrange, Act
        boolean shouldWait = PlanRunner.shouldWaitForPendingPlan(null, 83_490L);

        // Assert
        assertFalse(shouldWait);
    }

    @Test
    void hasCalendarDayMismatch_returnsFalseWhenPlanAndCurrentDayTimeShareCalendarDay() {
        // Plan generated for a farmer's 04:30 wake on calendar day 1; current dayTime is 06:30
        // the same morning. Calendar day matches.
        // Arrange
        DayPlan plan = DayPlan.builder()
                .dayType(PlanDayType.WORK_DAY)
                .calendarDay(1L)
                .schedule(schedule(TimeOfDay.AT_04_30.getCivilTick()))
                .build();
        long currentDayTime = TimeOfDay.TICKS_PER_DAY + TimeOfDay.AT_06_30.getMinecraftTick();

        // Act
        boolean mismatch = PlanRunner.hasCalendarDayMismatch(plan, currentDayTime);

        // Assert
        assertFalse(mismatch);
    }

    @Test
    void hasCalendarDayMismatch_returnsTrueWhenPlayerTimeSetCrossesMidnight() {
        // Reproduces the original bug: plan generated for calendar day 0 (e.g. a farmer woke at
        // 04:30 the very first morning), then /time set jumps the world to 06:30 of calendar day 1.
        // The stored plan no longer matches the active calendar day.
        // Arrange
        DayPlan plan = DayPlan.builder()
                .dayType(PlanDayType.WORK_DAY)
                .calendarDay(0L)
                .schedule(schedule(TimeOfDay.AT_04_30.getCivilTick()))
                .build();
        long dayTimeOnNextCalendarDay = TimeOfDay.TICKS_PER_DAY + TimeOfDay.AT_06_30.getMinecraftTick();

        // Act
        boolean mismatch = PlanRunner.hasCalendarDayMismatch(plan, dayTimeOnNextCalendarDay);

        // Assert
        assertTrue(mismatch);
    }

    @Test
    void hasCalendarDayMismatch_returnsTrueForBedSleepSkipFromYesterdayDuskToTodayDawn() {
        // Bed-sleep skip: plan was generated yesterday morning (wake at 07:00 calendar day 0),
        // player slept through the night, dayTime jumps to dawn of calendar day 1. Calendar flipped
        // even though the plan's authored duration may not yet be exhausted.
        // Arrange
        DayPlan plan = DayPlan.builder()
                .dayType(PlanDayType.WORK_DAY)
                .calendarDay(0L)
                .schedule(schedule(TimeOfDay.AT_07_00.getCivilTick()))
                .build();
        long dawnOfNextCalendarDay = TimeOfDay.TICKS_PER_DAY;

        // Act
        boolean mismatch = PlanRunner.hasCalendarDayMismatch(plan, dawnOfNextCalendarDay);

        // Assert
        assertTrue(mismatch);
    }

    @Test
    void hasActiveCurrentSlot_returnsTrueWhenCurrentSlotActive() {
        // Arrange
        DayPlan plan = plan(slot(1_000, 600, PlanSlotStatus.ACTIVE));

        // Act
        boolean active = PlanRunner.hasActiveCurrentSlot(plan);

        // Assert
        assertTrue(active);
    }

    @Test
    void hasActiveCurrentSlot_returnsFalseWhenCurrentSlotPending() {
        // Arrange
        DayPlan plan = plan(slot(1_000, 600, PlanSlotStatus.PENDING));

        // Act
        boolean active = PlanRunner.hasActiveCurrentSlot(plan);

        // Assert
        assertFalse(active);
    }

    @Test
    void hasActiveCurrentSlot_returnsFalseWhenPlanNull() {
        // Arrange, Act
        boolean active = PlanRunner.hasActiveCurrentSlot(null);

        // Assert
        assertFalse(active);
    }

    private static DayPlan plan(PlanSlot... slots) {
        DayPlan.DayPlanBuilder builder = DayPlan.builder()
                .dayType(PlanDayType.WORK_DAY)
                .calendarDay(1L)
                .schedule(schedule());
        for (PlanSlot slot : slots) {
            builder.slot(slot);
        }
        return builder.build();
    }

    private static PlanSlot slot(int startTick, int durationTicks, PlanSlotStatus status) {
        return slot(startTick, durationTicks, true, status);
    }

    private static PlanSlot slot(int startTick, int durationTicks, boolean flexible, PlanSlotStatus status) {
        return PlanSlot.builder()
                .startTick(startTick)
                .behaviorKey(BehaviorKey.TRADE_INITIATE)
                .priority(1)
                .flexible(flexible)
                .estimatedDurationTicks(durationTicks)
                .status(status)
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
