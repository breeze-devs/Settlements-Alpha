package dev.breezes.settlements.domain.ai.planning;

/**
 * Who produced a {@link PlanArrival}.
 */
public enum PlanAuthor {

    /**
     * The deterministic plan generator ({@code HeuristicPlanGenerator}), submitted async via
     * {@code PlanRunner#submitNextPlanAsync}.
     */
    HEURISTIC,

    /**
     * The always-on per-villager LLM overlay ({@code LlmOverlayPlanGenerator}), enqueued at plan
     * exhaustion and submitted via {@code PlanRequestService}.
     */
    LLM

}
