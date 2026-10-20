package dev.breezes.settlements.domain.settlement.model;

import dev.breezes.settlements.domain.time.ClockTicks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementFadingTest {

    /**
     * A step of the test's own, standing in for the health cadence's interval.
     */
    private static final ClockTicks STEP = ClockTicks.of(1_200L);

    /**
     * A duration three steps long.
     */
    private static final ClockTicks THREE_STEPS = ClockTicks.of(3 * STEP.getTicks());

    @Test
    void begun_isNotSpent_evenByAOneTickDuration() {
        // Act
        boolean spent = SettlementFading.BEGUN.isSpent(ClockTicks.ONE);

        // Assert
        assertFalse(spent);
    }

    @Test
    void advance_addsTheStepToTheTimeSpent() {
        // Act
        SettlementFading advanced = new SettlementFading(ClockTicks.of(100L)).advance(ClockTicks.of(50L));

        // Assert
        assertEquals(new SettlementFading(ClockTicks.of(150L)), advanced);
    }

    @Test
    void advance_returnsTheSameFading_forAStepOfZero() {
        // Arrange
        SettlementFading fading = new SettlementFading(ClockTicks.of(100L));

        // Act
        SettlementFading advanced = fading.advance(ClockTicks.ZERO);

        // Assert
        assertSame(fading, advanced);
    }

    @Test
    void advance_rejectsANegativeStep() {
        // Act, Assert
        assertThrows(IllegalArgumentException.class, () -> SettlementFading.BEGUN.advance(ClockTicks.of(-1L)));
    }

    @Test
    void isSpent_notOneStepBeforeTheDuration() {
        // Arrange
        SettlementFading twoSteps = SettlementFading.BEGUN.advance(STEP).advance(STEP);

        // Act
        boolean spent = twoSteps.isSpent(THREE_STEPS);

        // Assert
        assertFalse(spent);
    }

    @Test
    void isSpent_atExactlyTheDuration() {
        // Arrange
        SettlementFading threeSteps = SettlementFading.BEGUN.advance(STEP).advance(STEP).advance(STEP);

        // Act
        boolean spent = threeSteps.isSpent(THREE_STEPS);

        // Assert
        assertTrue(spent);
    }

    @Test
    void isSpent_notOneTickBeforeTheDuration() {
        // Act
        boolean spent = new SettlementFading(ClockTicks.of(THREE_STEPS.getTicks() - 1)).isSpent(THREE_STEPS);

        // Assert
        assertFalse(spent);
    }

    @Test
    void isSpent_byADurationShortenedBelowTheTimeAlreadySpent() {
        // Arrange — the duration shrank to one step while the settlement had faded for three
        SettlementFading threeSteps = new SettlementFading(THREE_STEPS);

        // Act
        boolean spent = threeSteps.isSpent(STEP);

        // Assert
        assertTrue(spent);
    }

    @Test
    void isSpent_rejectsADurationOfZero() {
        // Act, Assert — a zero duration would disband every settlement the moment it began fading
        assertThrows(IllegalArgumentException.class, () -> SettlementFading.BEGUN.isSpent(ClockTicks.ZERO));
    }

    @Test
    void rejectsANegativeTimeSpent() {
        // Act, Assert
        assertThrows(IllegalArgumentException.class, () -> new SettlementFading(ClockTicks.of(-1L)));
    }

}
