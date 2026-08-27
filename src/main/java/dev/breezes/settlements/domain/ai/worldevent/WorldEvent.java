package dev.breezes.settlements.domain.ai.worldevent;

import lombok.Builder;
import lombok.Getter;
import net.minecraft.core.SectionPos;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable envelope for a single event on the {@link WorldEventBus}.
 */
@Getter
@Builder
public final class WorldEvent {

    /**
     * Monotonic sequence number assigned by the bus at append-time
     */
    private final long sequence;

    /**
     * Game tick at the moment of emission
     */
    private final long gameTick;

    /**
     * Semantic type of the event
     */
    private final WorldEventType type;

    /**
     * UUID of the entity that caused this event (the actor / emitter)
     */
    private final UUID actorId;

    /**
     * UUID of the primary target entity, if any
     * e.g. for TRADE_INVITE_SENT this is the invite receiver's UUID
     */
    @Nullable
    private final UUID targetId;

    /**
     * X coordinate of the event origin
     */
    private final double posX;

    /**
     * Y coordinate of the event origin
     */
    private final double posY;

    /**
     * Z coordinate of the event origin
     */
    private final double posZ;

    /**
     * Chunk-section X of the actor position at emission time
     */
    private final int chunkX;

    /**
     * Chunk-section Z of the actor position at emission time
     */
    private final int chunkZ;

    /**
     * Registry id carried by offer/exclusive events
     * Null for events that do not have a backing registry entry
     */
    @Nullable
    private final UUID registryId;

    /**
     * Optional string metadata (e.g. the behavior key that produced the event)
     * Kept as a plain string to avoid pulling domain types into the envelope.
     */
    @Nullable
    private final String metadata;

    /**
     * Optional structured outcome of the event.
     * Absent outcome is interpreted as {@link EventOutcome#SUCCESS} by all downstream
     * consumers, so existing events that do not set this field are unaffected.
     */
    @Nullable
    private final EventOutcome outcome;

    /**
     * Optional free-text reason fragment explaining the outcome.
     * Meaningful when {@link #outcome} is {@link EventOutcome#FAILURE}.
     * Example: "no bed available", "haggling fell through", "no one responded".
     */
    @Nullable
    private final String reason;

    /**
     * Optional free-text detail string for enriching seed phrasing.
     * Example: "3 melons", "4 bread for 1 emerald".
     * Distinct from {@link #metadata} which carries structured behavior keys.
     */
    @Nullable
    private final String detail;

    /**
     * Optional structured detail map for LLM phrasing (e.g. item, count, price).
     * Serialized by {@link dev.breezes.settlements.domain.ai.perception.ObservationFactory}
     * as {@code "detail.*"} metadata keys on promotion to episodic memory.
     * Null when no structured detail was recorded.
     */
    @Nullable
    private final Map<String, String> detailFields;

    /**
     * Optional content-addressed deduplication key for cross-witness convergence.
     * <p>
     * When present, {@link dev.breezes.settlements.domain.ai.perception.ObservationFactory}
     * uses this UUID directly as the observation id instead of the actor-keyed derivation.
     * Two villagers that independently witness the same subject in the same coarse
     * spatial cell and time bucket emit events with the same dedupeKey, so both perceive one
     * origin id and a store admitting the second recognizes the fact it already holds.
     * <p>
     * Null for events other than sightings, which use the witness-keyed derivation instead.
     */
    @Nullable
    private final UUID dedupeKey;

    /**
     * Convenience builder that derives chunk coordinates from world position.
     */
    public static WorldEventBuilder fromPos(double x, double y, double z) {
        return WorldEvent.builder()
                .posX(x)
                .posY(y)
                .posZ(z)
                .chunkX(SectionPos.blockToSectionCoord((int) Math.floor(x)))
                .chunkZ(SectionPos.blockToSectionCoord((int) Math.floor(z)));
    }

}
