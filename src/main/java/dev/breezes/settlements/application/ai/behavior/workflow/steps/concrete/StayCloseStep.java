package dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete;

import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.Targetable;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetableType;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.AbstractStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.BehaviorStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.domain.ai.brain.ISettlementsBrainEntity;
import lombok.Builder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Gates an action step behind a distance check: if the initiator is close enough to the current
 * {@link BehaviorStateType#TARGET}, ticks the action step; otherwise ticks the navigate step, unless
 * the target has been lost.
 */
public class StayCloseStep<T extends ISettlementsBrainEntity> extends AbstractStep<T> {

    private static final String DEFAULT_NAME = "StayCloseStep";

    private final double closeEnoughDistanceSquared;
    private final NavigateToTargetStep<T> navigateStep;
    private final BehaviorStep<T> actionStep;

    /**
     * Stage to transition to if the target is an entity that no longer exists.
     * Set to null to keep approaching its last position.
     */
    @Nullable
    private final StageKey targetLostTransition;

    @Builder
    private StayCloseStep(@Nullable String name,
                          double closeEnoughDistance,
                          @Nonnull NavigateToTargetStep<T> navigateStep,
                          @Nonnull BehaviorStep<T> actionStep,
                          @Nullable StageKey targetLostTransition,
                          int timeoutTicks,
                          @Nullable StageKey timeoutTransition) {
        super(name == null ? DEFAULT_NAME : name, timeoutTicks, timeoutTransition);
        this.closeEnoughDistanceSquared = closeEnoughDistance * closeEnoughDistance;
        this.navigateStep = navigateStep;
        this.actionStep = actionStep;
        this.targetLostTransition = targetLostTransition;
    }

    @Override
    protected StepResult doTick(@Nonnull BehaviorContext<T> context) {
        if (this.isCloseEnough(context)) {
            return this.actionStep.tick(context);
        }
        if (this.targetLostTransition != null && this.isTargetLost(context)) {
            return StepResult.transition(this.targetLostTransition);
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

    private boolean isTargetLost(@Nonnull BehaviorContext<T> context) {
        return context.getState(BehaviorStateType.TARGET, TargetState.class)
                .flatMap(TargetState::getFirst)
                .filter(target -> target.getType() == TargetableType.ENTITY)
                .map(target -> !target.getAsEntity().isAlive())
                .orElse(false);
    }

}
