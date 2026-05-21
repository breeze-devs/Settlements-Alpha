package dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete;

import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.Targetable;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.AbstractStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.domain.ai.brain.ISettlementsBrainEntity;
import dev.breezes.settlements.domain.world.location.Location;
import lombok.Builder;

import javax.annotation.Nonnull;
import java.util.Optional;

public class NavigateToTargetStep<T extends ISettlementsBrainEntity> extends AbstractStep<T> {

    private final float speed;
    private final int completionDistance;

    @Builder
    public NavigateToTargetStep(float speed, int completionDistance) {
        super("NavigateToTargetStep");
        this.speed = speed;
        this.completionDistance = completionDistance;
    }

    @Override
    protected StepResult doTick(@Nonnull BehaviorContext<T> context) {
        T initiator = context.getInitiator();

        if (initiator.getNavigationManager().isNavigating()) {
            return StepResult.noOp();
        }

        Optional<Location> target = context.getState(BehaviorStateType.TARGET, TargetState.class)
                .flatMap(TargetState::getFirst)
                .map(Targetable::getLocation);
        if (target.isEmpty()) {
            return StepResult.noOp();
        }

        initiator.getNavigationManager().navigateTo(target.get(), this.speed, this.completionDistance);
        return StepResult.noOp();
    }

}
