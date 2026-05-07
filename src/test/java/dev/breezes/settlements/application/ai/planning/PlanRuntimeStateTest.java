package dev.breezes.settlements.application.ai.planning;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

}
