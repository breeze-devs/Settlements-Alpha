package dev.breezes.settlements.models.actions;

import dev.breezes.settlements.models.conditions.ICondition;

import java.util.Map;

/**
 * Represents a step in an action plan
 */
public interface IActionStep<T> {

    IAction<T> getAction();

    /**
     * Returns a map of conditions to actions that should be taken if the condition is met
     * <p>
     * If multiple conditions are met, a random one will be chosen (due to the nature of Maps)
     * TODO: should we consider a priority system?
     */
    Map<ICondition<T>, IAction<T>> getTransitions();

}
