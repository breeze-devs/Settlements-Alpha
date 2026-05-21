package dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete;

import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.Targetable;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.AbstractStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.BehaviorStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.domain.ai.brain.ISettlementsBrainEntity;
import lombok.Builder;
import lombok.CustomLog;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Gates an action step behind a distance check: if the initiator is close enough to the current
 * {@link BehaviorStateType#TARGET}, ticks the action step; otherwise ticks the navigate step.
 * <p>
 * This is a thin composite around two child steps — it intentionally does not extend
 * {@code ConditionalStep}. Conceptually it is "a step that gates its action behind a distance
 * condition," which is more specific than the general conditional dispatch contract.
 */
@CustomLog
public class StayCloseStep<T extends ISettlementsBrainEntity> extends AbstractStep<T> {

    private static final String DEFAULT_NAME = "StayCloseStep";

    private final double closeEnoughDistanceSquared;
    private final NavigateToTargetStep<T> navigateStep;
    private final BehaviorStep<T> actionStep;

    @Builder
    private StayCloseStep(@Nullable String name,
                          double closeEnoughDistance,
                          @Nonnull NavigateToTargetStep<T> navigateStep,
                          @Nonnull BehaviorStep<T> actionStep,
                          int timeoutTicks,
                          @Nullable StageKey timeoutTransition) {
        super(name == null ? DEFAULT_NAME : name, timeoutTicks, timeoutTransition);
        this.closeEnoughDistanceSquared = closeEnoughDistance * closeEnoughDistance;
        this.navigateStep = navigateStep;
        this.actionStep = actionStep;
    }

    @Override
    protected StepResult doTick(@Nonnull BehaviorContext<T> context) {
        if (this.isCloseEnough(context)) {
            return this.actionStep.tick(context);
        }
        return this.navigateStep.tick(context);
    }

    @Override
    protected void doOnEnter() {
        this.navigateStep.onEnter();
        this.actionStep.onEnter();
    }

    @Override
    protected void doReset() {
        this.navigateStep.reset();
        this.actionStep.reset();
    }

    private boolean isCloseEnough(@Nonnull BehaviorContext<T> context) {
        T initiator = context.getInitiator();
        return context.getState(BehaviorStateType.TARGET, TargetState.class)
                .flatMap(TargetState::getFirst)
                .map(Targetable::getLocation)
                .map(target -> target.distanceSquared(initiator.getMinecraftEntity()) < this.closeEnoughDistanceSquared)
                .orElse(false);
    }

}
