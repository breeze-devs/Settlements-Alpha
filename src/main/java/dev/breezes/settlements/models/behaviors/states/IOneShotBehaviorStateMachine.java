package dev.breezes.settlements.models.behaviors.states;

import java.util.List;

public interface IOneShotBehaviorStateMachine {

    List<IBehaviorState> getStates();

    IBehaviorStateDuration getTotalDurationInTicks();

}
