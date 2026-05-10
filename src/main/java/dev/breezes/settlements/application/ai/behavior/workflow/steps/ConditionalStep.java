package dev.breezes.settlements.application.ai.behavior.workflow.steps;

import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.domain.ai.brain.ISettlementsBrainEntity;
import dev.breezes.settlements.domain.ai.conditions.ICondition;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;

/**
 * Represents a step that executes different paths based on a condition
 */
@AllArgsConstructor
public class ConditionalStep<T extends ISettlementsBrainEntity> implements BehaviorStep<T> {

    private final ICondition<BehaviorContext<T>> condition;

    private final BehaviorStep<T> trueStep;
    private final BehaviorStep<T> falseStep;

    @Override
    public StepResult tick(@Nonnull BehaviorContext<T> stateHolder) {
        if (this.condition.test(stateHolder)) {
            return this.trueStep.tick(stateHolder);
        } else {
            return this.falseStep.tick(stateHolder);
        }
    }

    @Override
    public void reset() {
        this.trueStep.reset();
        this.falseStep.reset();
    }

}
