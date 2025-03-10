package dev.breezes.settlements.models.behaviors.steps;

import dev.breezes.settlements.models.behaviors.stages.Stage;
import dev.breezes.settlements.models.behaviors.states.BehaviorContext;
import dev.breezes.settlements.models.conditions.ICondition;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Represents a step that executes different paths based on a condition
 */
@AllArgsConstructor
public class ConditionalStep implements BehaviorStep {

    private final ICondition<BehaviorContext> condition;

    private final BehaviorStep trueStep;
    private final BehaviorStep falseStep;

    @Override
    public Optional<Stage> tick(@Nonnull BehaviorContext stateHolder) {
        if (this.condition.test(stateHolder)) {
            return this.trueStep.tick(stateHolder);
        } else {
            return this.falseStep.tick(stateHolder);
        }
    }

}
