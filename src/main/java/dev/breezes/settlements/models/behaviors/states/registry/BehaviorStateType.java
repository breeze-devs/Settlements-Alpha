package dev.breezes.settlements.models.behaviors.states.registry;

import dev.breezes.settlements.models.behaviors.states.BehaviorState;
import dev.breezes.settlements.models.behaviors.states.registry.items.ItemState;
import dev.breezes.settlements.models.behaviors.states.registry.targets.TargetState;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BehaviorStateType {

    TARGET("target", TargetState.class),
    ITEMS_TO_PICK_UP("items_to_pick_up", ItemState.class),
    SPEECH_BUBBLE("speech_bubble", SpeechBubbleState.class),
    ;

    private final String name;
    private final Class<? extends BehaviorState> clazz;

}
