package dev.breezes.settlements.domain.ai.catalog;

/**
 * Abstract resource channels that a behavior claims while executing.
 * <p>
 * Two behaviors are concurrently executable if and only if their required channel sets
 * are disjoint. Channel declarations live on {@link BehaviorPlanningMetadata#getRequiredChannels()}
 * and are used for conflict detection.
 */
public enum BehaviorChannel {

    /**
     * Locomotion and pathfinding
     */
    MOVEMENT,

    /**
     * Physical manipulation of items, blocks, or entities
     */
    INTERACTION,

    /**
     * Mental bandwidth — concentration and focused attention
     * The key differentiator between routine chores ({@code COGNITION}-free, compatible
     * with social overlay) and focused work such as enchanting, mining, or taming, which
     * blocks concurrent social interaction.
     */
    COGNITION,

    /**
     * Interpersonal communication — conversation or negotiation
     */
    SOCIAL,
    ;

}
