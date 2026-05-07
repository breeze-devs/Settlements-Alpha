package dev.breezes.settlements.domain.ai.planning;

/**
 * Lifecycle state of a {@link DayPlan}.
 * <p>
 * A plan begins {@code PENDING}, moves to {@code ACTIVE} when the first slot starts
 * executing, may be {@code SUSPENDED} during a reactive override (e.g. PANIC, RAID),
 * and ends as {@code COMPLETED} when all slots have been processed.
 */
public enum PlanStatus {

    /**
     * Plan generated but execution has not yet started.
     */
    PENDING,

    /**
     * At least one slot is actively being executed.
     */
    ACTIVE,

    /**
     * Execution is temporarily paused due to a reactive override (e.g. PANIC, RAID).
     */
    SUSPENDED,

    /**
     * All slots have been executed, skipped, or interrupted — the plan is exhausted.
     */
    COMPLETED,
    ;

}
