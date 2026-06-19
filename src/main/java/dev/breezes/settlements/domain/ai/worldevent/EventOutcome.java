package dev.breezes.settlements.domain.ai.worldevent;

/**
 * Structured outcome of a world event.
 * <p>
 * Carried as an optional field on {@link WorldEvent}. Absent outcome is treated as
 * {@link #SUCCESS} downstream so every existing event renders exactly as today.
 * <p>
 * {@link #FAILURE} signals that the event represents a completed *attempt* that did not
 * succeed (e.g. a trade that fell through, a courtship that was rejected). The projector
 * uses this to render "tried to ... but ..." phrasing rather than a completed-act clause.
 */
public enum EventOutcome {

    /**
     * The event completed successfully. Default interpretation when outcome is absent.
     */
    SUCCESS,

    /**
     * The event represents a completed attempt that did not produce the intended result.
     */
    FAILURE,

}
