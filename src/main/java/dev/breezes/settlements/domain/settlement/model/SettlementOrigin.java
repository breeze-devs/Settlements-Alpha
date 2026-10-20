package dev.breezes.settlements.domain.settlement.model;

/**
 * How a settlement came to exist.
 */
public enum SettlementOrigin {

    /**
     * Generated with its structure, whose structure start still carries the settlement.
     */
    GENERATED,

    /**
     * Founded at a bell.
     */
    FOUNDED

}
