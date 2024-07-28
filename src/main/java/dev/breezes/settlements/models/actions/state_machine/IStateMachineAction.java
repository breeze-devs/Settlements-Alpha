package dev.breezes.settlements.models.actions.state_machine;

import dev.breezes.settlements.models.conditions.ICondition;

public interface IStateMachineAction<T> {

    ICondition<T> getTransitionCondition();

    void tick(int delta);

    // TODO: boolean canTransition() ?
    // TODO: or is this covered by the condition?

}
