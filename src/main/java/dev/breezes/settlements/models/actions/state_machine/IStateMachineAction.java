package dev.breezes.settlements.models.actions.state_machine;

import dev.breezes.settlements.models.actions.IAction;
import dev.breezes.settlements.models.conditions.ICondition;

public interface IStateMachineAction<T> extends IAction<T> {

    ICondition<T> getTransitionCondition();

    // TODO: boolean canTransition() ?
    // TODO: or is this covered by the condition?

}
