package dev.breezes.settlements.models.behaviors.states;

import java.util.Optional;

public interface IBehaviorState {

    Optional<IBehaviorState> getNextState();

    IBehaviorStateDuration getDurationInTicks();

}
