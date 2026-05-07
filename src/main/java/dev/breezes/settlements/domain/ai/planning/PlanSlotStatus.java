package dev.breezes.settlements.domain.ai.planning;

/**
 * Execution state of a single {@link PlanSlot}.
 * <p>
 * Normal transition: {@code PENDING → ACTIVE → COMPLETED}. A flexible slot may
 * move directly {@code PENDING → SKIPPED} when its behavior's preconditions fail.
 * A rigid slot (e.g. eating, sleeping) stays {@code PENDING} and is retried each tick
 * until preconditions pass — it is never automatically skipped.
 */
public enum PlanSlotStatus {

    /**
     * Slot is queued and has not yet started.
     */
    PENDING,

    /**
     * The associated behavior is currently running.
     */
    ACTIVE,

    /**
     * The behavior ran to natural completion.
     */
    COMPLETED,

    /**
     * The slot was bypassed because its behavior's preconditions failed and the slot
     * is {@link PlanSlot#isFlexible() flexible}. Rigid slots are never skipped.
     */
    SKIPPED,

    /**
     * The behavior was stopped mid-execution by a reactive override.
     */
    INTERRUPTED,
    ;

}
