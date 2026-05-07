package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.ai.planning.DayPlanActivityBlock;
import dev.breezes.settlements.domain.ai.planning.DayPlanActivityContext;
import dev.breezes.settlements.domain.ai.planning.DayPlanSchedule;
import dev.breezes.settlements.domain.ai.planning.PlanSlot;
import dev.breezes.settlements.domain.ai.planning.PlanSlotStatus;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
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
                .generatedForDay(1L)
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
                .generatedForDay(1L)
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
                .generatedForDay(1L)
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
                .generatedForDay(1L)
                .schedule(schedule())
                .dayStartTick(0)
                .build());
    }

    private static DayPlan plan(PlanSlot... slots) {
        DayPlan.DayPlanBuilder builder = DayPlan.builder()
                .dayType(PlanDayType.WORK_DAY)
                .generatedForDay(1L)
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
