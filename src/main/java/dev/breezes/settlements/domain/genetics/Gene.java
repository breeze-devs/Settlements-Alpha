package dev.breezes.settlements.domain.genetics;

/**
 * Value object representing a single gene's potential.
 * Invariants: Value must be between 0.0f and 1.0f.
 */
public record Gene(double value) {

    public Gene {
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException("Gene value must be between 0.0 and 1.0, got: " + value);
        }
    }

    public boolean isTierReached(float threshold) {
        return this.value >= threshold;
    }

    public boolean isDebuffActive(float threshold) {
        return this.value <= threshold;
    }

}
