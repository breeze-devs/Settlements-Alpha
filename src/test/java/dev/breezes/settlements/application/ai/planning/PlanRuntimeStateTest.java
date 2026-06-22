package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.ai.planning.DayPlanSchedule;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class PlanRuntimeStateTest {

    @Test
    void advanceClock_firstTickReportsLoadAndPrimesDayTime() {
        // Arrange
        PlanRuntimeState runtime = new PlanRuntimeState();

        // Act
        DeltaResult result = runtime.advanceClock(12_000L);

        // Assert
        assertTrue(result.firstTick());
        assertFalse(result.backwardJump());
        assertEquals(1, result.deltaTicks());
        assertEquals(0L, result.rawDelta());
        assertEquals(-1L, result.previousDayTime());
        assertEquals(12_000L, runtime.getPreviousPlanTickDayTime());
    }

    @Test
    void advanceClock_sameDayTimeReportsFrozenDaylight() {
        // Arrange
        PlanRuntimeState runtime = new PlanRuntimeState();
        runtime.advanceClock(5_000L);

        // Act
        DeltaResult result = runtime.advanceClock(5_000L);

        // Assert
        assertFalse(result.firstTick());
        assertFalse(result.backwardJump());
        assertEquals(0, result.deltaTicks());
        assertEquals(0L, result.rawDelta());
        assertEquals(5_000L, result.previousDayTime());
    }

    @Test
    void advanceClock_forwardJumpReturnsRawPositiveDelta() {
        // Arrange
        PlanRuntimeState runtime = new PlanRuntimeState();
        runtime.advanceClock(5_000L);

        // Act
        DeltaResult result = runtime.advanceClock(5_600L);

        // Assert
        assertFalse(result.firstTick());
        assertFalse(result.backwardJump());
        assertEquals(600, result.deltaTicks());
        assertEquals(600L, result.rawDelta());
        assertEquals(5_000L, result.previousDayTime());
    }

    @Test
    void advanceClock_backwardJumpReportsNegativeRawDelta() {
        // Arrange
        PlanRuntimeState runtime = new PlanRuntimeState();
        runtime.advanceClock(18_000L);

        // Act
        DeltaResult result = runtime.advanceClock(1_000L);

        // Assert
        assertFalse(result.firstTick());
        assertTrue(result.backwardJump());
        assertEquals(0, result.deltaTicks());
        assertEquals(-17_000L, result.rawDelta());
        assertEquals(18_000L, result.previousDayTime());
    }

    @Test
    void reset_clearsPendingPlanExhaustionAndAsyncState() {
        // Arrange
        PlanRuntimeState runtime = new PlanRuntimeState();
        DayPlan pendingPlan = plan(24_000L);
        CompletableFuture<DayPlan> future = new CompletableFuture<>();
        runtime.setPendingNextPlan(pendingPlan);
        runtime.markPlanExhausted();
        runtime.setPendingFuture(future);
        runtime.setPendingFutureSubmittedAtDayTime(12_000L);
        runtime.setPendingFutureWakeAtAbsoluteTick(24_000L);
        runtime.getPendingArrivals().offer(pendingPlan);

        // Act
        runtime.reset(13_000L);

        // Assert
        assertNull(runtime.getPendingNextPlan());
        assertFalse(runtime.isPlanExhausted());
        assertNull(runtime.getPendingFuture());
        assertTrue(future.isCancelled());
        assertTrue(runtime.getPendingArrivals().isEmpty());
        assertEquals(13_000L, runtime.getPreviousPlanTickDayTime());
    }

    @Test
    void incrementCurrentBehaviorElapsedTicks_accumulatesDelta() {
        // Arrange
        PlanRuntimeState runtime = new PlanRuntimeState();

        // Act
        runtime.incrementCurrentBehaviorElapsedTicks(10);
        runtime.incrementCurrentBehaviorElapsedTicks(5);

        // Assert
        assertEquals(15, runtime.getCurrentBehaviorElapsedTicks());
    }

    @Test
    void assignPlanBehavior_resetsElapsedCounter() {
        // Arrange
        PlanRuntimeState runtime = new PlanRuntimeState();
        runtime.incrementCurrentBehaviorElapsedTicks(500);

        // Act
        runtime.assignPlanBehavior(stubBehavior(), null);

        // Assert — a fresh assignment always starts from zero regardless of prior accumulation
        assertEquals(0, runtime.getCurrentBehaviorElapsedTicks());
    }

    @Test
    void clearCurrentBehavior_resetsElapsedCounter() {
        // Arrange
        PlanRuntimeState runtime = new PlanRuntimeState();
        runtime.incrementCurrentBehaviorElapsedTicks(200);

        // Act
        runtime.clearCurrentBehavior();

        // Assert
        assertEquals(0, runtime.getCurrentBehaviorElapsedTicks());
    }

    @Test
    void reset_resetsElapsedCounter() {
        // Arrange
        PlanRuntimeState runtime = new PlanRuntimeState();
        runtime.incrementCurrentBehaviorElapsedTicks(300);

        // Act
        runtime.reset(5_000L);

        // Assert
        assertEquals(0, runtime.getCurrentBehaviorElapsedTicks());
    }

    @Test
    void clearCurrentBehavior_doesNotClearPendingPlanOrExhaustion() {
        // Arrange
        PlanRuntimeState runtime = new PlanRuntimeState();
        DayPlan pendingPlan = plan(24_000L);
        runtime.setPendingNextPlan(pendingPlan);
        runtime.markPlanExhausted();

        // Act
        runtime.clearCurrentBehavior();

        // Assert
        assertSame(pendingPlan, runtime.getPendingNextPlan());
        assertTrue(runtime.isPlanExhausted());
    }

    @SuppressWarnings("unchecked")
    private static IBehavior<BaseVillager> stubBehavior() {
        return mock(IBehavior.class);
    }

    private static DayPlan plan(long wakeAtAbsoluteTick) {
        return DayPlan.builder()
                .dayType(PlanDayType.WORK_DAY)
                .wakeAtAbsoluteTick(wakeAtAbsoluteTick)
                .schedule(DayPlanSchedule.builder()
                        .wakeTick(0)
                        .bedtimeTick(12_000)
                        .build())
                .build();
    }

}
