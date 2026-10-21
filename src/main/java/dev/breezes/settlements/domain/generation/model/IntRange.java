package dev.breezes.settlements.domain.generation.model;

public record IntRange(int minInclusive, int maxInclusive) {

    public IntRange {
        if (maxInclusive < minInclusive) {
            throw new IllegalArgumentException("maxInclusive must be >= minInclusive");
        }
    }

    public static IntRange of(int minInclusive, int maxInclusive) {
        return new IntRange(minInclusive, maxInclusive);
    }

    public boolean contains(int value) {
        return value >= this.minInclusive && value <= this.maxInclusive;
    }

    public int clamp(int value) {
        return Math.clamp(value, this.minInclusive, this.maxInclusive);
    }

    public int size() {
        return this.maxInclusive - this.minInclusive + 1;
    }

}
