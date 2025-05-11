package dev.breezes.settlements.models.behaviors.steps.concrete;

import dev.breezes.settlements.entities.ISettlementsVillager;
import dev.breezes.settlements.models.behaviors.states.BehaviorContext;
import dev.breezes.settlements.models.behaviors.states.registry.BehaviorStateType;
import dev.breezes.settlements.models.behaviors.states.registry.targets.TargetState;
import dev.breezes.settlements.models.behaviors.states.registry.targets.Targetable;
import dev.breezes.settlements.models.behaviors.steps.BehaviorStep;
import dev.breezes.settlements.models.behaviors.steps.ConditionalStep;
import lombok.Builder;
import lombok.CustomLog;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Wrapper over an action step that ensures the initiator stays close to the target
 * <p>
 * If the initiator is not close enough to the target, the navigate step is executed.
 * Otherwise, the action step is executed.
 */
@CustomLog
public class StayCloseStep extends ConditionalStep {

    @Builder
    public StayCloseStep(double closeEnoughDistance, @Nonnull NavigateToTargetStep navigateStep, @Nonnull BehaviorStep actionStep) {
        super(context -> isCloseEnough(context, closeEnoughDistance * closeEnoughDistance),
                actionStep, navigateStep);
    }

    private static boolean isCloseEnough(@Nullable BehaviorContext context, double closeEnoughDistanceSquared) {
        if (context == null) {
            log.behaviorWarn("Behavior context is null");
            return false;
        }

        ISettlementsVillager initiator = context.getInitiator();
        return context.getState(BehaviorStateType.TARGET, TargetState.class)
                .flatMap(TargetState::getFirst)
                .map(Targetable::getLocation)
                .map(target -> target.distanceSquared(initiator.getMinecraftEntity()) < closeEnoughDistanceSquared)
                .orElse(false);
    }

}
