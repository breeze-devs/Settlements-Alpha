package dev.breezes.settlements.models.actions.plan;

import java.util.Optional;

/**
 * Represents a step in an action plan
 */
public interface IActionStep<T> {

    /**
     * Should be called to progress the action
     */
    void tick(int delta);

    boolean isComplete();

    /**
     * Returns the next action to be taken, or empty if this step is the last
     * <p>
     * If multiple conditions are met, a random one will be chosen (due to the nature of Maps)
     * TODO: should we consider a priority system?
     */
    Optional<IActionStep<T>> getNextAction();

}
