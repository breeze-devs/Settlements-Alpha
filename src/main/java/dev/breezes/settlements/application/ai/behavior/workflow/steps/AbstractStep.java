package dev.breezes.settlements.application.ai.behavior.workflow.steps;

import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.domain.ai.brain.ISettlementsBrainEntity;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Base class for behavior steps.
 * <p>
 * Subclasses override {@link #doTick(BehaviorContext)} for per-tick logic and (optionally)
 * {@link #doOnEnter()} / {@link #doReset()} for state cleanup. The base class handles:
 * <ul>
 *     <li>Counting ticks since the most recent {@link #onEnter()} — exposed implicitly through the
 *     timeout safety net.</li>
 *     <li>Enforcing an optional safety-net timeout: when {@code timeoutTicks > 0} and the elapsed-tick
 *     count exceeds it, the step short-circuits by returning either a transition to
 *     {@code timeoutTransition} (if set) or {@link StepResult#fail(String)} (clean failure that lets
 *     the plan runner advance the day). A bare timeout with no transition wired must fail rather than
 *     silently "complete" — a step that ran out of time did not succeed.
 *     Any non-positive {@code timeoutTicks} disables the check.</li>
 *     <li>Splitting reset semantics into per-entry ({@link #onEnter()}) and per-run ({@link #reset()})
 *     so that primitives like {@code LoopBackStep} can hold state that survives stage re-entries within
 *     a single behavior run but resets between runs.</li>
 * </ul>
 * Timeouts are a safety net, not a primary mechanism. Most steps drive completion through their own
 * primary logic (e.g. a tickable, a one-shot action) and leave the timeout disabled by using
 * {@link #AbstractStep(String)}.
 */
public abstract class AbstractStep<T extends ISettlementsBrainEntity> implements BehaviorStep<T> {

    /**
     * Sentinel value disabling the timeout safety net. Any value {@code <= 0} disables the check.
     */
    public static final int NO_TIMEOUT = -1;

    @Getter
    private final String name;
    @Getter
    private final UUID uuid;

    private final int timeoutTicks;
    @Nullable
    private final StageKey timeoutTransition;
    private int elapsedTicks;

    /**
     * Convenience constructor for steps that do not opt in to the timeout safety net.
     */
    protected AbstractStep(@Nonnull String name) {
        this(name, NO_TIMEOUT, null);
    }

    protected AbstractStep(@Nonnull String name,
                           int timeoutTicks,
                           @Nullable StageKey timeoutTransition) {
        this.name = name;
        this.uuid = UUID.randomUUID();
        this.timeoutTicks = timeoutTicks;
        this.timeoutTransition = timeoutTransition;
        this.elapsedTicks = 0;
    }

    @Override
    public final StepResult tick(@Nonnull BehaviorContext<T> context) {
        this.elapsedTicks++;
        if (this.timeoutTicks > 0 && this.elapsedTicks > this.timeoutTicks) {
            // When a transition target is wired, redirect (allows in-behavior reselection or recovery).
            // Otherwise fail cleanly so the behavior stops and the plan runner can advance the day.
            return this.timeoutTransition != null
                    ? StepResult.transition(this.timeoutTransition)
                    : StepResult.fail("timed out");
        }
        return this.doTick(context);
    }

    protected abstract StepResult doTick(@Nonnull BehaviorContext<T> context);

    /**
     * Per-stage-entry reset. Clears the framework-managed elapsed-tick counter then delegates to
     * {@link #doOnEnter()} for subclass per-entry state.
     */
    @Override
    public final void onEnter() {
        this.elapsedTicks = 0;
        this.doOnEnter();
    }

    /**
     * Per-behavior-run reset. Invokes {@link #onEnter()} (so a full reset implies fresh entry state)
     * then {@link #doReset()} for subclass per-run state — cross-entry state like loop counters.
     */
    @Override
    public final void reset() {
        this.onEnter();
        this.doReset();
    }

    /**
     * Subclasses override to clear per-stage-entry state (tickables, sequence indices, nested state
     * machine pointers). The framework-managed elapsed-tick counter is already cleared before this runs.
     */
    protected void doOnEnter() {
        // no-op by default
    }

    /**
     * Subclasses override to clear per-behavior-run state — counters that must persist across stage
     * re-entries within a run but reset between runs (e.g. {@code LoopBackStep.remaining}).
     */
    protected void doReset() {
        // no-op by default
    }

}
