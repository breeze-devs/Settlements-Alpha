package dev.breezes.settlements.models.actions.plan;

/**
 * Represents a directed, potentially-cyclic graph of actions
 */
public interface IActionPlan<T> {

    IActionStep<T> getStartingStep();

    void tick(int delta);

    boolean isComplete();

}
