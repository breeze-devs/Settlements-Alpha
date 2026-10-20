package dev.breezes.settlements.domain.settlement.model;

import dev.breezes.settlements.domain.time.ClockTicks;
import org.apache.commons.lang3.Validate;

/**
 * How long a settlement has been fading, counted in loaded ticks.
 *
 * @param elapsed the loaded time spent fading; not negative
 */
public record SettlementFading(ClockTicks elapsed) {

    /**
     * A fading that has just begun.
     */
    public static final SettlementFading BEGUN = new SettlementFading(ClockTicks.ZERO);

    public SettlementFading {
        Validate.notNull(elapsed, "Fading time must not be null");
        Validate.isTrue(elapsed.getTicks() >= 0, "Fading time must not be negative: %d", elapsed.getTicks());
    }

    /**
     * Returns the fading after another step of loaded time, reusing this instance for a step of zero.
     *
     * @throws IllegalArgumentException when step is negative
     */
    public SettlementFading advance(ClockTicks step) {
        Validate.isTrue(step.getTicks() >= 0, "Fading step must not be negative: %d", step.getTicks());
        if (step.getTicks() == 0) {
            return this;
        }
        return new SettlementFading(ClockTicks.of(Math.addExact(this.elapsed.getTicks(), step.getTicks())));
    }

    /**
     * Returns whether the fading has lasted at least the given duration.
     *
     * @throws IllegalArgumentException when duration is not positive
     */
    public boolean isSpent(ClockTicks duration) {
        // A duration of zero would read every fading as spent the moment it began
        Validate.isTrue(duration.getTicks() > 0, "Fading duration must be positive: %d", duration.getTicks());
        return this.elapsed.getTicks() >= duration.getTicks();
    }

}
