package dev.breezes.settlements.domain.common;

/**
 * A boolean that changes only after the opposing observation repeats a fixed number of times in a row.
 * <p>
 * A single agreeing observation restarts the opposing run, so a source that flickers between the two
 * values never commits either.
 */
public final class DebouncedSignal {

    private final int observationsToChange;
    private boolean value;
    private int opposingObservations;

    /**
     * @param observationsToChange consecutive opposing observations required to change; at least 1, where
     *                             1 is no debouncing
     */
    public DebouncedSignal(boolean initialValue, int observationsToChange) {
        if (observationsToChange < 1) {
            throw new IllegalArgumentException("observationsToChange must be at least 1, got " + observationsToChange);
        }

        this.observationsToChange = observationsToChange;
        this.value = initialValue;
        this.opposingObservations = 0;
    }

    /**
     * Feeds one observation in and returns the value that stands after it.
     */
    public boolean observe(boolean observation) {
        if (observation == this.value) {
            this.opposingObservations = 0;
            return this.value;
        }

        this.opposingObservations++;
        if (this.opposingObservations >= this.observationsToChange) {
            this.value = observation;
            this.opposingObservations = 0;
        }
        return this.value;
    }

    public boolean value() {
        return this.value;
    }

}
