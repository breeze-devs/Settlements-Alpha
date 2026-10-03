package dev.breezes.settlements.application.ai.override;

import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;

import javax.annotation.Nonnull;

/**
 * The result of an {@link OverridePolicy} evaluation: what the policy asks to install in the override slot.
 */
public sealed interface OverrideRequest {

    /**
     * A catalog behavior, validated against its preconditions before it is installed.
     *
     * @param behaviorKey the behavior to install
     */
    record CatalogBehavior(@Nonnull BehaviorKey behaviorKey) implements OverrideRequest {
    }

    /**
     * An unstarted runner the policy built itself, installed as is.
     * <p>
     * Its tier must be its policy's tier, since arbitration compares the running tier to decide preemption.
     *
     * @param runner the runner to install
     */
    record PreparedRunner(@Nonnull OverrideRunner runner) implements OverrideRequest {
    }

}
