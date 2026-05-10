package dev.breezes.settlements.domain.ai.catalog;

/**
 * A single entry in a {@link ProfessionBehaviorPool}, declaring that the owning profession
 * can perform the referenced behavior.
 * <p>
 * The weight is a relative scheduling preference, not a hard slot count. A weight of zero keeps
 * the behavior available to explicit systems while excluding it from routine heuristic packing.
 */
public record PoolEntry(BehaviorKey key, int weight) {

    public PoolEntry {
        if (weight < 0) {
            throw new IllegalArgumentException("PoolEntry weight must be >= 0, got " + weight);
        }
    }

    public static PoolEntry of(BehaviorKey key) {
        return new PoolEntry(key, 1);
    }

    public static PoolEntry of(BehaviorKey key, int weight) {
        return new PoolEntry(key, weight);
    }

}
