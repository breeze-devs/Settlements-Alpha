package dev.breezes.settlements.domain.ai.override;

/**
 * Override priority tiers, declared from highest to lowest.
 */
public enum OverrideTier {

    EMERGENCY,
    REACTIVE,
    OPPORTUNISTIC;

    /**
     * Whether this tier has strictly higher priority than other.
     */
    public boolean isAbove(OverrideTier other) {
        return this.compareTo(other) < 0;
    }

}
