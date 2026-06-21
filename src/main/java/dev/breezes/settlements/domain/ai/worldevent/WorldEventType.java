package dev.breezes.settlements.domain.ai.worldevent;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Typed, centrally registered event types for the {@link WorldEventBus}.
 */
@Getter
@RequiredArgsConstructor
public enum WorldEventType {

    /**
     * A villager started executing a plan behavior. Carries the behavior key as metadata.
     */
    BEHAVIOR_STARTED(WorldEventNamespace.WORLD),

    /**
     * A villager finished a plan behavior normally. Carries the behavior key as metadata.
     */
    BEHAVIOR_COMPLETED(WorldEventNamespace.WORLD),

    /**
     * A villager's behavior terminated without accomplishing its intended deed. Carries the
     * behavior key as metadata and an optional reason.
     */
    BEHAVIOR_FAILED(WorldEventNamespace.WORLD),

    SHEEP_SHEARED(WorldEventNamespace.WORLD),
    SHEEP_DYED(WorldEventNamespace.WORLD),
    CROP_HARVESTED(WorldEventNamespace.WORLD),

    /**
     * A trade negotiation was completed (deal or walk-away).
     * Carries the session registry id in the {@link WorldEvent#getRegistryId()} field.
     * First-accept-wins resolution goes through {@code TradeSessionRegistry}, not the bus.
     */
    TRADE_COMPLETED(WorldEventNamespace.WORLD),

    /**
     * A courtship event was completed.
     * Carries the session registry id in the {@link WorldEvent#getRegistryId()} field.
     * First-accept-wins resolution goes through {@code CourtshipSessionRegistry}, not the bus.
     */
    COURTSHIP_COMPLETED(WorldEventNamespace.WORLD),

    /**
     * A courtship advance was turned down. The actor is the receiver who declined;
     * the target is the spurned presenter. Only the receiver knows why it was rejected, so this is
     * emitted from the accept-side behavior. Carries the session registry id.
     */
    COURTSHIP_REJECTED(WorldEventNamespace.WORLD),

    /**
     * This villager sent a trade invite to a target.
     * Carries the session registry id so the receiver can correlate with the registry.
     */
    TRADE_INVITE_SENT(WorldEventNamespace.WORLD),

    /**
     * This villager sent a courtship invite to a target.
     * Carries the session registry id so the receiver can correlate with the registry.
     */
    COURTSHIP_INVITE_SENT(WorldEventNamespace.WORLD),

    /**
     * An Investigate behavior confirmed the hearsay claim: the claimed condition was found
     * to be true at the tip location. The actor is the investigating villager.
     */
    TIP_CONFIRMED(WorldEventNamespace.WORLD),

    /**
     * An Investigate behavior refuted the hearsay claim: the claimed condition was not found
     * at the tip location. The actor is the investigating villager.
     */
    TIP_REFUTED(WorldEventNamespace.WORLD),

    /**
     * The villager's current day plan was invalidated and will be regenerated.
     */
    DAY_PLAN_INVALIDATED(WorldEventNamespace.SYSTEM),

    /**
     * The villager's day plan was fully exhausted.
     */
    PLAN_EXHAUSTED(WorldEventNamespace.SYSTEM),
    ;

    private final WorldEventNamespace namespace;

    /**
     * Salient terminal deeds a villager always remembers about its own actions — the durable record
     * of what a behavior run accomplished. Generic lifecycle completion/failure is excluded: it is
     * low-signal background noise and would otherwise flood the bounded knowledge store with
     * "finished a task" entries. Those still pass through the importance gate, which keeps them
     * below the promotion threshold.
     * <p>
     * Keeping this classification next to the event registry prevents perception, inference, and
     * future memory compaction from drifting as new terminal deeds are added.
     */
    public boolean isSelfRememberableTerminalEvent() {
        return switch (this) {
            case SHEEP_SHEARED, SHEEP_DYED, CROP_HARVESTED,
                 TRADE_COMPLETED, COURTSHIP_COMPLETED, COURTSHIP_REJECTED,
                 TIP_CONFIRMED, TIP_REFUTED -> true;
            case BEHAVIOR_STARTED, BEHAVIOR_COMPLETED, BEHAVIOR_FAILED,
                 TRADE_INVITE_SENT, COURTSHIP_INVITE_SENT,
                 DAY_PLAN_INVALIDATED, PLAN_EXHAUSTED -> false;
        };
    }

}
