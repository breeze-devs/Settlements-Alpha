package dev.breezes.settlements.models.behaviors.states;

import dev.breezes.settlements.util.SettlementsException;

/**
 * Represents one instance of a behavior's state
 */
@Deprecated
public interface ConstantBehaviorState extends BehaviorState {

    default void reset() {
        throw new SettlementsException("Cannot reset a constant state");
    }

    default ConstantBehaviorState getDefaultState() {
        return this;
    }

}
