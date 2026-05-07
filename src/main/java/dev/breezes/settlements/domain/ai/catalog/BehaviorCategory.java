package dev.breezes.settlements.domain.ai.catalog;

/**
 * Broad classification of a behavior's purpose within a villager's daily routine.
 */
public enum BehaviorCategory {

    /**
     * Productive labor tied to the villager's profession (farming, smithing, enchanting).
     */
    WORK,

    /**
     * Interaction with other villagers (gossip, trading, cooperation).
     */
    SOCIAL,

    /**
     * Personal upkeep (eating, resting, healing).
     */
    SELF_CARE,

    /**
     * Leisure and unstructured activity (wandering, relaxing).
     */
    LEISURE,

    /**
     * Combat-oriented activities (fighting, defending, slaying).
     */
    COMBAT,
    ;

}
