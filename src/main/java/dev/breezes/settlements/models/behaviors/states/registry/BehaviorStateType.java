package dev.breezes.settlements.models.behaviors.states.registry;

import dev.breezes.settlements.models.behaviors.states.BehaviorState;
import dev.breezes.settlements.models.behaviors.states.registry.targets.TargetState;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BehaviorStateType {

    TARGET("target", TargetState.class),
    ;

    private final String name;
    private final Class<? extends BehaviorState> clazz;

}
