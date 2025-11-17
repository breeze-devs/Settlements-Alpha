package dev.breezes.settlements.models.behaviors.steps.concrete;

import dev.breezes.settlements.entities.villager.ISettlementsVillager;
import dev.breezes.settlements.models.behaviors.stages.Stage;
import dev.breezes.settlements.models.behaviors.states.BehaviorContext;
import dev.breezes.settlements.models.behaviors.states.registry.BehaviorStateType;
import dev.breezes.settlements.models.behaviors.states.registry.targets.TargetState;
import dev.breezes.settlements.models.behaviors.states.registry.targets.Targetable;
import dev.breezes.settlements.models.behaviors.steps.AbstractStep;
import dev.breezes.settlements.models.location.Location;
import lombok.Builder;

import javax.annotation.Nonnull;
import java.util.Optional;

public class NavigateToTargetStep extends AbstractStep {

    private final float speed;
    private final int completionDistance;

    @Builder
    public NavigateToTargetStep(float speed, int completionDistance) {
        super("NavigateToTargetStep");
        this.speed = speed;
        this.completionDistance = completionDistance;
    }

    @Override
    public Optional<Stage> tick(@Nonnull BehaviorContext context) {
        ISettlementsVillager initiator = context.getInitiator();

        // TODO: Don't navigate if already navigating
        // TODO: this is causing the villager to run back to the workstation due to vanilla logic
        // TODO: re-enable this check after fixing the issue
//        if (initiator.getNavigationManager().isNavigating()) {
//            return Optional.empty();
//        }

        Optional<Location> target = context.getState(BehaviorStateType.TARGET, TargetState.class)
                .flatMap(TargetState::getFirst)
                .map(Targetable::getLocation);
        if (target.isEmpty()) {
            return Optional.empty();
        }

        initiator.getNavigationManager().navigateTo(target.get(), this.speed, this.completionDistance);
        return Optional.empty();
    }

}
