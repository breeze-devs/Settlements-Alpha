package dev.breezes.settlements.application.ai.behavior.workflow.steps;

import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.domain.ai.brain.ISettlementsBrainEntity;

import javax.annotation.Nonnull;

/**
 * Defines the contract for a step in a behavior
 */
public interface BehaviorStep<T extends ISettlementsBrainEntity> {

    StepResult tick(@Nonnull BehaviorContext<T> context);

    default void reset() {
        // no-op by default
    }

}
