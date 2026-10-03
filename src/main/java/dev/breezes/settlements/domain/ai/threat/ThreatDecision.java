package dev.breezes.settlements.domain.ai.threat;

/**
 * Which response rule decided one threat assessment. Both combat decisions reach the COMBAT verdict and differ only in
 * which option fights; both panic decisions reach the PANIC verdict and differ only in whether this assessment found
 * the villager alarmed.
 */
public enum ThreatDecision {

    /**
     * The previously selected option keeps fighting.
     */
    CONTINUE_COMBAT,

    /**
     * The first option in precedence order that can engage a sighted hostile starts fighting.
     */
    ENTER_COMBAT,

    /**
     * The villager is alarmed, and flees or shelters.
     */
    PANIC,

    /**
     * A panicked villager is no longer alarmed, but was too recently to calm down while a hostile is still in
     * awareness; it keeps fleeing or sheltering.
     */
    LINGER_IN_PANIC,

    /**
     * Nothing requires a response.
     */
    HOLD

}
