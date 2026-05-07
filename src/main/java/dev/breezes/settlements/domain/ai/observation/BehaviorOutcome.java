package dev.breezes.settlements.domain.ai.observation;

/**
 * The result of a behavior execution, reported in a {@link BehaviorCompletionEvent}.
 * <p>
 * Outcomes feed back into the observation buffer and ultimately influence future
 * planning through the distillation pipeline. Failures and interruptions score higher
 * in importance than successes because unexpected events carry more information.
 */
public enum BehaviorOutcome {

    /**
     * Behavior ran to natural completion and achieved its goal.
     */
    SUCCESS,

    /**
     * Behavior started but could not complete its goal.
     */
    FAILURE,

    /**
     * Behavior could not start because its preconditions were not satisfied.
     */
    PRECONDITION_FAILED,

    /**
     * Behavior was stopped mid-execution by an external event (e.g. a reactive override).
     */
    INTERRUPTED,
    ;

}
