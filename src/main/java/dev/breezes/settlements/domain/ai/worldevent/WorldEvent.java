package dev.breezes.settlements.domain.ai.worldevent;

import lombok.Builder;
import lombok.Getter;
import net.minecraft.core.SectionPos;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Immutable envelope for a single event on the {@link WorldEventBus}.
 * <p>
 * Design notes:
 * <ul>
 *   <li>{@link #sequence} is a monotonically increasing integer assigned by the bus at append time.
 *       Per-villager cursors store {@code lastSeenSeq} and consume only the delta each tick.</li>
 *   <li>{@link #chunkX} and {@link #chunkZ} enable cheap Manhattan-distance rejection in the
 *       Phase 4 perception gate — no world-lookup needed at filter time.</li>
 *   <li>{@link #registryId} is non-null only for offer/exclusive events (trade invite, courtship
 *       invite). First-accept-wins resolution goes through the owning registry, never the bus.
 *       Events are announcements; registries are state of record.</li>
 * </ul>
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
     * Semantic type, including namespace classification
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
     * Optional string metadata (e.g. behavior key for BEHAVIOR_STARTED / BEHAVIOR_COMPLETED)
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
