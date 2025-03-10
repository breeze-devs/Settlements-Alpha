package dev.breezes.settlements.models.behaviors.states;

/**
 * Represents one instance of a behavior's state
 */
public interface BehaviorState {

    /**
     * Reset the state to its initial state
     * <p>
     * Often called upon initialization or clean-up
     */
    void reset();

}
