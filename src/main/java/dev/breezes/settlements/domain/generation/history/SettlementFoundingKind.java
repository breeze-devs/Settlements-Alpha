package dev.breezes.settlements.domain.generation.history;

/**
 * Whether a settlement was founded before its record began or as its record began.
 */
public enum SettlementFoundingKind {

    /**
     * Founded before its record began, with a past of its own: a settlement generated with its structure, or one
     * founded in a village that stood before anyone recorded it.
     */
    ESTABLISHED,

    /**
     * Founded as its record began: a settlement players built.
     */
    NEW

}
