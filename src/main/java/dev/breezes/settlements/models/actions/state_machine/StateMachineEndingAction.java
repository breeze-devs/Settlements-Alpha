package dev.breezes.settlements.models.actions.state_machine;

import dev.breezes.settlements.models.conditions.ICondition;
import lombok.CustomLog;

@CustomLog
public class StateMachineEndingAction<T> implements IStateMachineAction<T> {

    @Override
    public IStateMachineAction<T> getNextAction() {
        return this;
    }

    @Override
    public ICondition<T> getTransitionCondition() {
        // Since this is the ending action, we will never transition to another action
        return t -> false;
    }

    @Override
    public void tickAction(int delta) {
        // Do nothing
        log.trace("Ticking ending action with delta %d", delta);
    }

}
