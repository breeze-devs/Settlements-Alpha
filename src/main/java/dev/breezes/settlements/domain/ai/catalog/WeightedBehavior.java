package dev.breezes.settlements.domain.ai.catalog;

/**
 * A behavior descriptor paired with the profession-pool weight that should shape routine planning.
 */
public record WeightedBehavior(BehaviorPlanningMetadata descriptor, int weight) {

    public WeightedBehavior {
        if (weight < 0) {
            throw new IllegalArgumentException("WeightedBehavior weight must be >= 0, got " + weight);
        }
    }

    public BehaviorKey key() {
        return this.descriptor.getKey();
    }

}
