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
     * A notable resource mutation: sheep was sheared.
     */
    SHEEP_SHEARED(WorldEventNamespace.WORLD),

    /**
     * A notable resource mutation: crop was harvested.
     */
    CROP_HARVESTED(WorldEventNamespace.WORLD),

    /**
     * A social act: a trade negotiation was completed (deal or walk-away).
     * Carries the session registry id in the {@link WorldEvent#getRegistryId()} field.
     * First-accept-wins resolution goes through {@code TradeSessionRegistry}, not the bus.
     */
    TRADE_COMPLETED(WorldEventNamespace.WORLD),

    /**
     * A social act: a courtship event was completed.
     * Carries the session registry id in the {@link WorldEvent#getRegistryId()} field.
     * First-accept-wins resolution goes through {@code CourtshipSessionRegistry}, not the bus.
     */
    COURTSHIP_COMPLETED(WorldEventNamespace.WORLD),

    /**
     * A social act: this villager sent a trade invite to a target.
     * Carries the session registry id so the receiver can correlate with the registry.
     */
    TRADE_INVITE_SENT(WorldEventNamespace.WORLD),

    /**
     * A social act: this villager sent a courtship invite to a target.
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
     * Infrastructure signal — never passed to the perception gate.
     */
    DAY_PLAN_INVALIDATED(WorldEventNamespace.SYSTEM),

    /**
     * The villager's day plan was fully exhausted.
     * Infrastructure signal — never passed to the perception gate.
     */
    PLAN_EXHAUSTED(WorldEventNamespace.SYSTEM),
    ;

    private final WorldEventNamespace namespace;

}
