package dev.breezes.settlements.application.ai.courtship;

import dev.breezes.settlements.domain.ai.knowledge.KnowledgeEntry;
import dev.breezes.settlements.domain.ai.observation.ObservationMetadataKeys;
import dev.breezes.settlements.domain.ai.observation.ObservationType;
import dev.breezes.settlements.domain.ai.worldevent.EventOutcome;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Builds a first-hand {@link KnowledgeEntry} for a private courtship failure without touching
 * the {@link dev.breezes.settlements.domain.ai.worldevent.WorldEventBus}.
 * <p>
 * Private failures (e.g. the initiator waited for an accept that never came) are unobservable
 * by bystanders, so they must not appear on the bus. The villager should still remember the
 * attempt as a direct observation (hop = 0, source = null) so the monologue projector can
 * surface it as a personal failure seed.
 * <p>
 * Separating the entry construction from the Minecraft-bound wiring ({@link CourtshipSelfMemoryRecorder})
 * keeps this class dependency-free and unit-testable.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class CourtshipSelfMemoryEntryBuilder {

    /**
     * Weight assigned to a self-recorded private courtship failure.
     * <p>
     * Matches the base importance that {@link dev.breezes.settlements.domain.ai.perception.ObservationFactory}
     * assigns to {@link WorldEventType#COURTSHIP_COMPLETED} events on the bus (2.5F), so the villager
     * weights its own first-hand memory of the failed attempt as seriously as it would weight a
     * witnessed courtship event. Staleness and corroboration mechanics apply normally thereafter.
     */
    public static final float SELF_FAILURE_WEIGHT = 2.5F;

    /**
     * Constructs a first-hand knowledge entry recording that this villager's courtship attempt
     * failed with the given reason, using a random origin id (off-bus, cannot corroborate).
     *
     * @param actorId             UUID of the villager who initiated and experienced the failure
     * @param partnerId           UUID of the courtship target, or null if already gone
     * @param currentTick         game tick at which the failure is being recorded
     * @param reason              human-readable reason the attempt failed (e.g. "no one answered")
     * @param originObservationId stable UUID for deduplication; callers should supply a random UUID
     * @return a first-hand {@link KnowledgeEntry} ready to admit into the villager's own store
     */
    public static KnowledgeEntry build(@Nonnull UUID actorId,
                                       @Nullable UUID partnerId,
                                       long currentTick,
                                       @Nonnull String reason,
                                       @Nonnull UUID originObservationId) {
        Map<String, String> metadata = buildMetadata(actorId, reason);
        String content = buildContent(actorId, reason);

        return KnowledgeEntry.fromDirectObservation(originObservationId, content, ObservationType.SOCIAL,
                currentTick, currentTick, partnerId, metadata, SELF_FAILURE_WEIGHT);
    }

    /**
     * Materializes the metadata map the projector reads to render the seed phrase.
     * Mirrors what {@link dev.breezes.settlements.domain.ai.perception.ObservationFactory#metadataFor}
     * would produce for a bus-emitted courtship failure, using the same key constants.
     */
    public static Map<String, String> buildMetadata(@Nonnull UUID actorId, @Nonnull String reason) {
        Map<String, String> metadata = new HashMap<>(4);
        metadata.put(ObservationMetadataKeys.EVENT_TYPE, WorldEventType.COURTSHIP_COMPLETED.name());
        metadata.put(ObservationMetadataKeys.ACTOR_ID, actorId.toString());
        metadata.put(ObservationMetadataKeys.OUTCOME, EventOutcome.FAILURE.name());
        metadata.put(ObservationMetadataKeys.REASON, reason);
        return Map.copyOf(metadata);
    }

    /**
     * Builds a plain-text content string for this entry (used for debug/logging, not seed phrasing).
     * Seed phrasing is driven by the metadata fields, not this string.
     */
    public static String buildContent(@Nonnull UUID actorId, @Nonnull String reason) {
        return "courtship completed by " + actorId + " (" + reason + ")";
    }

}
