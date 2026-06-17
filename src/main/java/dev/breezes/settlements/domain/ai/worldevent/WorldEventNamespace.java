package dev.breezes.settlements.domain.ai.worldevent;

/**
 * Namespace classification for world events emitted onto the {@link WorldEventBus}.
 * <p>
 * Perception gates should skip {@link #SYSTEM} events: these carry infrastructure signals
 * (e.g. DayPlanInvalidated) that are not observable world facts. Only {@link #WORLD}
 * events can be admitted through the per-villager perception gate in Phase 4.
 */
public enum WorldEventNamespace {

    /**
     * Observable world-fact events: behavior completions, notable mutations (shear, harvest, trade),
     * and social acts. These may pass through the perception gate in Phase 4.
     */
    WORLD,

    /**
     * Internal infrastructure signals that are never observable by a villager.
     * Examples: DayPlanInvalidated, PlanExhausted. Perception gates must ignore these.
     */
    SYSTEM,
    ;

}
