package dev.breezes.settlements.domain.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DebouncedSignalTest {

    private static final int OBSERVATIONS_TO_CHANGE = 4;

    @Test
    void observe_holdsTheCurrentValueUntilTheOpposingRunIsOneShortOfComplete() {
        // Arrange
        DebouncedSignal signal = new DebouncedSignal(false, OBSERVATIONS_TO_CHANGE);

        // Act
        for (int i = 0; i < OBSERVATIONS_TO_CHANGE - 1; i++) {
            signal.observe(true);
        }

        // Assert: a run one observation short of the requirement must not have changed anything yet.
        assertFalse(signal.value());
    }

    @Test
    void observe_changesTheValueOnTheObservationThatCompletesTheRun() {
        // Arrange
        DebouncedSignal signal = new DebouncedSignal(false, OBSERVATIONS_TO_CHANGE);
        for (int i = 0; i < OBSERVATIONS_TO_CHANGE - 1; i++) {
            signal.observe(true);
        }

        // Act
        boolean valueAfterCompletingRun = signal.observe(true);

        // Assert
        assertTrue(valueAfterCompletingRun);
        assertTrue(signal.value());
    }

    @Test
    void observe_restartsTheOpposingRunWhenOneObservationDisagrees() {
        // Arrange: an almost-complete run, then a single observation agreeing with the standing value.
        DebouncedSignal signal = new DebouncedSignal(false, OBSERVATIONS_TO_CHANGE);
        for (int i = 0; i < OBSERVATIONS_TO_CHANGE - 1; i++) {
            signal.observe(true);
        }
        signal.observe(false);

        // Act: resume opposing, again stopping one short of the requirement.
        for (int i = 0; i < OBSERVATIONS_TO_CHANGE - 1; i++) {
            signal.observe(true);
        }

        // Assert: the interruption reset the count rather than leaving it accumulated, so the second
        // partial run is no closer to changing the value than the first was.
        assertFalse(signal.value());
    }

    @Test
    void observe_neverChangesUnderAnAlternatingSource() {
        // Arrange
        DebouncedSignal signal = new DebouncedSignal(false, OBSERVATIONS_TO_CHANGE);

        // Act: the flicker this type exists to absorb -- a source that never settles either way.
        for (int i = 0; i < OBSERVATIONS_TO_CHANGE * 4; i++) {
            signal.observe(i % 2 == 0);
        }

        // Assert
        assertFalse(signal.value());
    }

    @Test
    void observe_requiresAFullRunToChangeBackAfterAlreadyChangingOnce() {
        // Arrange: drive it to true, then start the opposing run back toward false.
        DebouncedSignal signal = new DebouncedSignal(false, OBSERVATIONS_TO_CHANGE);
        for (int i = 0; i < OBSERVATIONS_TO_CHANGE; i++) {
            signal.observe(true);
        }

        // Act
        for (int i = 0; i < OBSERVATIONS_TO_CHANGE - 1; i++) {
            signal.observe(false);
        }

        // Assert: the return trip is no cheaper than the outbound one -- a counter left at its ceiling
        // by the first change would let this partial run flip the value already.
        assertTrue(signal.value());
        assertFalse(signal.observe(false));
    }

    @Test
    void observe_leavesTheValueUntouchedWhileObservationsAgree() {
        // Arrange
        DebouncedSignal signal = new DebouncedSignal(true, OBSERVATIONS_TO_CHANGE);

        // Act
        for (int i = 0; i < OBSERVATIONS_TO_CHANGE * 2; i++) {
            signal.observe(true);
        }

        // Assert
        assertTrue(signal.value());
    }

    @Test
    void constructor_rejectsARunLengthThatWouldDefeatTheDebounce() {
        // Act, Assert: a requirement below one observation would let the value change on every
        // observation, which is the one configuration that silently makes this type do nothing.
        assertThrows(IllegalArgumentException.class, () -> new DebouncedSignal(false, 0));
        assertThrows(IllegalArgumentException.class, () -> new DebouncedSignal(false, -1));
    }

}
