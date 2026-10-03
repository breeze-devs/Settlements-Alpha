package dev.breezes.settlements.domain.ai.threat;

/**
 * The outcome of one threat assessment.
 */
public enum ThreatVerdict {

    /**
     * The villager fights its selected combat option.
     */
    COMBAT,

    /**
     * The villager flees or shelters.
     */
    PANIC,

    /**
     * Nothing requires a response.
     */
    HOLD

}
