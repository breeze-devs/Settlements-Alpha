package dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete;

import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.AbstractStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.domain.ai.brain.ISettlementsBrainEntity;
import lombok.Builder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.ToIntFunction;

/**
 * Decrement-then-branch terminator. On each tick it decrements an iteration counter and transitions back to
 * {@code loopBackTo} while iterations remain, otherwise transitions to {@code completionTransition}.
 * <p>
 * The iteration count is resolved lazily on the first tick of a behavior run via the
 * {@code maxIterationsResolver}, so the maximum may depend on per-run context (e.g. villager expertise).
 * <p>
 * Exhausting the loop transitions to an explicit {@code completionTransition} rather than calling
 * {@link StepResult#complete()} because inside a {@code StagedStep} {@code complete()} unwinds the
 * whole staged step to its parent's next stage — skipping every sibling stage downstream of the
 * loop (AWARD, cleanup, etc.). The explicit transition keeps multi-stage post-loop work reachable.
 */
public class LoopBackStep<T extends ISettlementsBrainEntity> extends AbstractStep<T> {

    private static final int UNINITIALIZED = -1;
    private static final int MINIMUM_ITERATIONS = 1;

    private final StageKey loopBackTo;
    private final StageKey completionTransition;
    private final ToIntFunction<BehaviorContext<T>> maxIterationsResolver;
    private int remaining;

    @Builder
    private LoopBackStep(@Nonnull String name,
                         @Nonnull StageKey loopBackTo,
                         @Nonnull StageKey completionTransition,
                         @Nonnull ToIntFunction<BehaviorContext<T>> maxIterationsResolver,
                         int timeoutTicks,
                         @Nullable StageKey timeoutTransition) {
        super(name, timeoutTicks, timeoutTransition);
        this.loopBackTo = loopBackTo;
        this.completionTransition = completionTransition;
        this.maxIterationsResolver = maxIterationsResolver;
        this.remaining = UNINITIALIZED;
    }

    @Override
    protected StepResult doTick(@Nonnull BehaviorContext<T> context) {
        if (this.remaining == UNINITIALIZED) {
            this.remaining = Math.max(MINIMUM_ITERATIONS, this.maxIterationsResolver.applyAsInt(context));
        }
        this.remaining--;
        return this.remaining > 0
                ? StepResult.transition(this.loopBackTo)
                : StepResult.transition(this.completionTransition);
    }

    @Override
    protected void doReset() {
        this.remaining = UNINITIALIZED;
    }

}
