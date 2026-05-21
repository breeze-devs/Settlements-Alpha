package dev.breezes.settlements.application.ai.behavior.workflow.steps;

import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.domain.ai.brain.ISettlementsBrainEntity;

import javax.annotation.Nonnull;

/**
 * Defines the contract for a step in a behavior.
 * <p>
 * <b>Lifecycle.</b> A step has two reset semantics:
 * <ul>
 *     <li>{@link #onEnter()} — called when this step becomes the current stage of a containing
 *     {@code StagedStep}. Clears per-stage-entry state: timeout counters, tickables, sequence
 *     indices, current-stage pointers in nested state machines. Cross-entry state (e.g. loop
 *     iteration counters) is NOT cleared.</li>
 *     <li>{@link #reset()} — called at behavior-run boundaries (start / stop). Clears all
 *     state including cross-entry counters. A full reset implies a fresh entry: by default,
 *     {@code AbstractStep#reset()} invokes {@code onEnter()} first.</li>
 * </ul>
 * The two-hook split exists because some primitives (most notably {@code LoopBackStep}) hold
 * state that must survive stage re-entries within a single behavior run but reset between runs.
 */
public interface BehaviorStep<T extends ISettlementsBrainEntity> {

    StepResult tick(@Nonnull BehaviorContext<T> context);

    /**
     * Per-stage-entry lifecycle hook. Override in {@link AbstractStep} subclasses via
     * {@code doOnEnter()}. Default is a no-op for lambda implementations.
     */
    default void onEnter() {
        // no-op by default
    }

    /**
     * Per-behavior-run lifecycle hook. Override in {@link AbstractStep} subclasses via
     * {@code doReset()}. Default is a no-op for lambda implementations.
     */
    default void reset() {
        // no-op by default
    }

}
