package dev.breezes.settlements.domain.genetics;

/**
 * Value object representing a single gene's potential.
 * Invariants: Value must be between 0.0f and 1.0f.
 */
public record Gene(double value) {

    public static final double MINIMUM_VALUE = 0.0;
    public static final double MAXIMUM_VALUE = 1.0;

    public Gene {
        if (value < MINIMUM_VALUE || value > MAXIMUM_VALUE) {
            throw new IllegalArgumentException("Gene value must be between " + MINIMUM_VALUE + " and "
                    + MAXIMUM_VALUE + ", got: " + value);
        }
    }

}
