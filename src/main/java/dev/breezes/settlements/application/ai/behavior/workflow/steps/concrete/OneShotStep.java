package dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete;

import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.AbstractStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.domain.ai.brain.ISettlementsBrainEntity;
import lombok.Builder;
import lombok.CustomLog;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Executes a single action on its first tick and produces a terminal result.
 * <p>
 * The action contract is that it must return a terminal {@link StepResult} — a {@link StepResult.Transition},
 * {@link StepResult.Complete}, {@link StepResult.Fail}, or {@link StepResult.Abort}. Returning
 * {@link StepResult.NoOp} is a misuse; the step warns and converts to {@link StepResult#complete()} rather
 * than stalling.
 */
@CustomLog
public class OneShotStep<T extends ISettlementsBrainEntity> extends AbstractStep<T> {

    @Nonnull
    private final Function<BehaviorContext<T>, StepResult> action;

    @Builder
    private OneShotStep(@Nonnull String name,
                        @Nonnull Function<BehaviorContext<T>, StepResult> action,
                        int timeoutTicks,
                        @Nullable StageKey timeoutTransition) {
        super(name, timeoutTicks, timeoutTransition);
        this.action = action;
    }

    @Override
    protected StepResult doTick(@Nonnull BehaviorContext<T> context) {
        StepResult result = this.action.apply(context);
        if (result instanceof StepResult.NoOp) {
            // Action contract violation
            log.warn("OneShotStep '{}' returned NoOp; the action must return a terminal result. Treating as complete.", this.getName());
            return StepResult.complete();
        }
        return result;
    }

}
