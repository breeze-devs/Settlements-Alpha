package dev.breezes.settlements.application.ai.dialogue;

/**
 * Priority tiers for queued dialog requests
 * <p>
 * Requests at higher tiers are serviced first and are last to be dropped when the queue
 * is over-capacity. Ambient requests are silently dropped if the queue is full.
 */
public enum DialoguePriority {

    AMBIENT,

    VILLAGER,

    PLAYER,
    ;

}
