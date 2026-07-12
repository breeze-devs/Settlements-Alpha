package dev.breezes.settlements.application.ai.inference.plan;

public enum PlanInferenceMode {

    /**
     * HeuristicPlanGenerator is the only source of day plans; no PLAN request is ever sent.
     */
    HEURISTIC,

    /**
     * The overnight PLAN sweep may overlay the heuristic plan with SIS-selected orderings.
     */
    LLM

}
