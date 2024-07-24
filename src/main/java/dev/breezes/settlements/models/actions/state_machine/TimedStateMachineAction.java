package dev.breezes.settlements.models.actions.state_machine;

import dev.breezes.settlements.models.conditions.ICondition;
import dev.breezes.settlements.models.misc.ITickable;
import lombok.Getter;

import javax.annotation.Nonnull;

/**
 * Represents a state machine action that has a hard time limit
 * <p>
 * i.e. the action will transition to the next action when the time limit is reached regardless of the transition condition
 */
@Getter
public abstract class TimedStateMachineAction<T> implements IStateMachineAction<T> {

    private final ICondition<T> originalTransitionCondition;
    private final ITickable actionTimeRemaining;

    protected TimedStateMachineAction(@Nonnull ICondition<T> originalTransitionCondition, @Nonnull ITickable actionTimeRemaining) {
        this.originalTransitionCondition = originalTransitionCondition;
        this.actionTimeRemaining = actionTimeRemaining;
    }

    @Override
    public void tick(int delta) {
        this.actionTimeRemaining.tick(delta);
        this.tickAction(delta);
    }

    /**
     * Should be overridden to perform the action
     */
    public abstract void tickAction(int delta);

    @Override
    public ICondition<T> getTransitionCondition() {
        return t -> this.actionTimeRemaining.isComplete() || this.originalTransitionCondition.test(t);
    }

}
