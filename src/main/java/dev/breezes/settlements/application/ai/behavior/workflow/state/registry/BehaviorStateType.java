package dev.breezes.settlements.application.ai.behavior.workflow.state.registry;

import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.blocks.VisitedBlockSitesState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.items.ItemState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.look.LookState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes.BehaviorOutcome;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetState;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BehaviorStateType {

    TARGET("target", TargetState.class),
    LOOK_TARGET("look_target", LookState.class),
    ITEMS_TO_PICK_UP("items_to_pick_up", ItemState.class),
    BEHAVIOR_OUTCOME("behavior_outcome", BehaviorOutcome.class),
    VISITED_BLOCK_SITES("visited_block_sites", VisitedBlockSitesState.class),
    ;

    private final String name;
    private final Class<? extends BehaviorState> clazz;

}
