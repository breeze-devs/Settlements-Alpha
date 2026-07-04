package dev.breezes.settlements.domain.ai.observation;

import dev.breezes.settlements.domain.ai.worldevent.EventOutcome;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Stable string keys for the metadata map materialized by
 * {@link dev.breezes.settlements.domain.ai.perception.ObservationFactory#metadataFor}.
 * <p>
 * Keys are shared between the producer side (ObservationFactory writes them) and the
 * consumer side (EpisodicEntryAssembler reads them). Keeping them in the domain layer
 * avoids a circular dependency and duplicated string literals.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ObservationMetadataKeys {

    /**
     * The {@link dev.breezes.settlements.domain.ai.worldevent.WorldEventType} name that
     * generated this observation. Always present.
     */
    public static final String EVENT_TYPE = "event_type";

    /**
     * The behavior key string carried by BEHAVIOR_STARTED/BEHAVIOR_COMPLETED events.
     * Present only when the originating world-event had a non-null metadata string.
     */
    public static final String EVENT_META = "event_meta";

    /**
     * UUID of the entity that caused the event. Present when the actor is known.
     */
    public static final String ACTOR_ID = "actor_id";

    /**
     * UUID of the session-registry entry backing offer/exclusive events.
     * Present only for events that carry a registry id.
     */
    public static final String REGISTRY_ID = "registry_id";

    /**
     * Optional free-text detail string set by a behavior to enrich seed phrasing.
     * Example value: "3 melons", "4 bread for 1 emerald".
     */
    public static final String DETAIL = "detail";

    /**
     * Prefix for structured detail sub-fields serialized into the flat metadata map.
     * Each structured key is stored as {@code "detail." + slotName} (e.g. {@code "detail.item"},
     * {@code "detail.count"}, {@code "detail.price"}).
     * The assembler strips this prefix when building the wire-level {@code detail} map.
     */
    public static final String DETAIL_PREFIX = "detail.";

    /**
     * Optional structured {@link EventOutcome} name set by a behavior.
     * Absent is treated as {@link EventOutcome#SUCCESS} downstream.
     */
    public static final String OUTCOME = "outcome";

    /**
     * Optional free-text reason fragment set by a behavior, meaningful when outcome is FAILURE.
     * Example: "no bed available", "haggling fell through".
     */
    public static final String REASON = "reason";

}
