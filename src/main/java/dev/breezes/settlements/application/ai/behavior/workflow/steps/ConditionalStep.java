package dev.breezes.settlements.application.ai.behavior.workflow.steps;

import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.domain.ai.brain.ISettlementsBrainEntity;
import dev.breezes.settlements.domain.ai.conditions.ICondition;
import lombok.Builder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Dispatches to one of two child steps based on a per-tick condition.
 */
public class ConditionalStep<T extends ISettlementsBrainEntity> extends AbstractStep<T> {

    private final ICondition<BehaviorContext<T>> condition;
    private final BehaviorStep<T> trueStep;
    private final BehaviorStep<T> falseStep;

    @Builder
    private ConditionalStep(@Nonnull String name,
                            @Nonnull ICondition<BehaviorContext<T>> condition,
                            @Nonnull BehaviorStep<T> trueStep,
                            @Nonnull BehaviorStep<T> falseStep,
                            int timeoutTicks,
                            @Nullable StageKey timeoutTransition) {
        super(name, timeoutTicks, timeoutTransition);
        this.condition = condition;
        this.trueStep = trueStep;
        this.falseStep = falseStep;
    }

    @Override
    protected StepResult doTick(@Nonnull BehaviorContext<T> context) {
        if (this.condition.test(context)) {
            return this.trueStep.tick(context);
        }
        return this.falseStep.tick(context);
    }

    @Override
    protected void doOnEnter() {
        this.trueStep.onEnter();
        this.falseStep.onEnter();
    }

    @Override
    protected void doReset() {
        this.trueStep.reset();
        this.falseStep.reset();
    }

}
