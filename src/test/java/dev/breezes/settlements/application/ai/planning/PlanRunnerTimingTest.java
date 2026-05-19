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
        boolean open = PlanRunner.isSlotWindowOpen(slot, 1_999, 0);

        // Assert
        assertFalse(open);
    }

    @Test
    void isSlotWindowOpen_returnsTrueAtStartWindow() {
        // Arrange
        PlanSlot slot = slot(2_000, 600, PlanSlotStatus.PENDING);

        // Act
        boolean open = PlanRunner.isSlotWindowOpen(slot, 2_000, 0);

        // Assert
        assertTrue(open);
    }

    @Test
    void isSlotWindowClosed_returnsFalseAtWindowEndBecauseEndIsInclusive() {
        // Arrange
        PlanSlot slot = slot(2_000, 600, PlanSlotStatus.PENDING);

        // Act
        boolean closed = PlanRunner.isSlotWindowClosed(slot, 2_600, 0);

        // Assert
        assertFalse(closed);
    }

    @Test
    void isSlotWindowClosed_returnsTrueAfterWindowEnd() {
        // Arrange
        PlanSlot slot = slot(2_000, 600, PlanSlotStatus.PENDING);

        // Act
        boolean closed = PlanRunner.isSlotWindowClosed(slot, 2_601, 0);

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
        PlanRunner.runSeekLoop(plan, 3_000, 0);

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
        PlanRunner.runSeekLoop(plan, 10_000, 0);

        // Assert
        assertEquals(PlanSlotStatus.ACTIVE, active.getStatus());
        assertEquals(PlanSlotStatus.PENDING, pending.getStatus());
        assertEquals(0, plan.getCurrentSlotIndex());
    }

    @Test
    void runSeekLoop_skipsPreWakeWrappedSlotsWhenCalledBeforeAuthoredDayStart() {
        // Arrange
        int authoredDayStart = 1_000;
        PlanSlot firstRestDaySlot = slot(1_500, 600, PlanSlotStatus.PENDING);
        DayPlan plan = DayPlan.builder()
                .slot(firstRestDaySlot)
                .dayType(PlanDayType.REST_DAY)
                .wakeAtAbsoluteTick(1L)
                .schedule(schedule(authoredDayStart))
                .dayStartTick(authoredDayStart)
                .build();

        // Act
        PlanRunner.runSeekLoop(plan, 0, authoredDayStart);

        // Assert
        assertEquals(PlanSlotStatus.SKIPPED, firstRestDaySlot.getStatus());
        assertEquals(1, plan.getCurrentSlotIndex());
    }

    @Test
    void detectOnLoadBackward_returnsTrueWhenLastExecutedSlotIsFutureRelativeToNow() {
        // Arrange
        DayPlan plan = DayPlan.builder()
                .slot(slot(4_000, 600, PlanSlotStatus.COMPLETED))
                .slot(slot(9_000, 600, PlanSlotStatus.PENDING))
                .dayType(PlanDayType.WORK_DAY)
                .wakeAtAbsoluteTick(1L)
                .schedule(schedule())
                .currentSlotIndex(1)
                .build();

        // Act
        boolean backward = PlanRunner.detectOnLoadBackward(plan, 2_000, 0);

        // Assert
        assertTrue(backward);
    }

    @Test
    void detectOnLoadBackward_returnsFalseWhenNoSlotsHaveExecuted() {
        // Arrange
        DayPlan plan = DayPlan.builder()
                .slot(slot(4_000, 600, PlanSlotStatus.PENDING))
                .dayType(PlanDayType.WORK_DAY)
                .wakeAtAbsoluteTick(1L)
                .schedule(schedule())
                .currentSlotIndex(0)
                .build();

        // Act
        boolean backward = PlanRunner.detectOnLoadBackward(plan, 2_000, 0);

        // Assert
        assertFalse(backward);
    }

    @Test
    void dayPlanBuilder_rejectsSlotWindowThatCrossesPlanDayBoundary() {
        // Arrange, Act, Assert
        assertThrows(IllegalArgumentException.class, () -> DayPlan.builder()
                .slot(slot(23_700, 500, PlanSlotStatus.PENDING))
                .dayType(PlanDayType.WORK_DAY)
                .wakeAtAbsoluteTick(1L)
                .schedule(schedule())
                .dayStartTick(0)
                .build());
    }

    @Test
    void shouldWaitForPendingPlan_returnsTrueBeforePendingWake() {
        // Arrange
        DayPlan pendingPlan = DayPlan.builder()
                .dayType(PlanDayType.WORK_DAY)
                .wakeAtAbsoluteTick(84_325L)
                .schedule(schedule())
                .build();

        // Act
        boolean shouldWait = PlanRunner.shouldWaitForPendingPlan(pendingPlan, 83_490L);

        // Assert
        assertTrue(shouldWait);
    }

    @Test
    void shouldWaitForPendingPlan_returnsFalseWhenPendingWakeArrived() {
        // Arrange
        DayPlan pendingPlan = DayPlan.builder()
                .dayType(PlanDayType.WORK_DAY)
                .wakeAtAbsoluteTick(84_325L)
                .schedule(schedule())
                .build();

        // Act
        boolean shouldWait = PlanRunner.shouldWaitForPendingPlan(pendingPlan, 84_325L);

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
                .wakeAtAbsoluteTick(TimeOfDay.AT_04_30.getTick())
                .schedule(schedule())
                .build();
        long currentDayTime = TimeOfDay.TICKS_PER_DAY + TimeOfDay.AT_06_30.getTick();

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
                .wakeAtAbsoluteTick(0L)
                .schedule(schedule())
                .build();
        long dayTimeOnNextCalendarDay = TimeOfDay.TICKS_PER_DAY + TimeOfDay.AT_06_30.getTick();

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
                .wakeAtAbsoluteTick(TimeOfDay.AT_07_00.getTick())
                .schedule(schedule())
                .build();
        long dawnOfNextCalendarDay = TimeOfDay.TICKS_PER_DAY;

        // Act
        boolean mismatch = PlanRunner.hasCalendarDayMismatch(plan, dawnOfNextCalendarDay);

        // Assert
        assertTrue(mismatch);
    }

    private static DayPlan plan(PlanSlot... slots) {
        DayPlan.DayPlanBuilder builder = DayPlan.builder()
                .dayType(PlanDayType.WORK_DAY)
                .wakeAtAbsoluteTick(1L)
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
                .reason("test")
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
                        .reason("test")
                        .build())
                .build();
    }

}
