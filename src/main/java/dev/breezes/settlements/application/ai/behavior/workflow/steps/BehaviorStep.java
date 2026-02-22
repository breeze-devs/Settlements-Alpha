package dev.breezes.settlements.application.ai.behavior.workflow.steps;

import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;

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
