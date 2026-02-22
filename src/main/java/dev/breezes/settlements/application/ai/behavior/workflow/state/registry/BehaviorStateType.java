package dev.breezes.settlements.application.ai.behavior.workflow.state.registry;

import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.items.ItemState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetState;
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
