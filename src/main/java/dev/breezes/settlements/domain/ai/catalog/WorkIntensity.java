package dev.breezes.settlements.domain.ai.catalog;

/**
 * Effort classification for a behavior
 */
public enum WorkIntensity {

    /**
     * High-effort labor
     */
    HEAVY,
    
    /**
     * Routine chores
     */
    LIGHT,

    /**
     * Basically no intensity, such as social or rest behaviors
     */
    NONE,
    ;

}
