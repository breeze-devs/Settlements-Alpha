package dev.breezes.settlements.domain.ai.catalog;

import javax.annotation.Nonnull;

/**
 * A behavior descriptor paired with the profession-pool weight that should shape routine planning.
 */
public record WeightedBehavior(@Nonnull BehaviorPlanningMetadata descriptor, int weight) {

    public WeightedBehavior {
        if (weight < 0) {
            throw new IllegalArgumentException("WeightedBehavior weight must be >= 0, got " + weight);
        }
    }

    public BehaviorKey key() {
        return this.descriptor.getKey();
    }

}
