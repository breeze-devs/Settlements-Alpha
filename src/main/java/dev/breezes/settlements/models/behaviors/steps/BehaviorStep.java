package dev.breezes.settlements.models.behaviors.steps;

import dev.breezes.settlements.models.behaviors.states.BehaviorContext;

import javax.annotation.Nonnull;

/**
 * Defines the contract for a step in a behavior
 */
public interface BehaviorStep {

    StepResult tick(@Nonnull BehaviorContext context);

    default void reset() {
        // no-op by default
    }

    // boolean canContinue(@Nonnull BehaviorContext context)

}
