package dev.breezes.settlements.domain.ai.catalog;

/**
 * A single entry in a {@link ProfessionBehaviorPool}, declaring that the owning profession
 * can perform the referenced behavior.
 * <p>
 * Intentionally minimal — the pool is a pure availability mapping. All scheduling intelligence
 * (weighting, ordering, rest-day filtering) lives in the heuristic planner, which reads
 * {@link BehaviorPlanningMetadata} intrinsics (category, intensity, channels) from the catalog.
 */
public record PoolEntry(BehaviorKey key) {

    public static PoolEntry of(BehaviorKey key) {
        return new PoolEntry(key);
    }

}
