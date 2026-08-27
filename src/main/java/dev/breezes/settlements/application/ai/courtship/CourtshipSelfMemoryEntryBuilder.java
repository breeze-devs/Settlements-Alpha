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
 * attempt first-hand so the monologue projector can surface it as a personal failure seed.
 * <p>
 * Separating the entry construction from the Minecraft-bound wiring ({@link CourtshipSelfMemoryRecorder})
 * keeps this class dependency-free and unit-testable.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class CourtshipSelfMemoryEntryBuilder {

    /**
     * Weight assigned to a self-recorded private courtship failure.
     */
    public static final float SELF_FAILURE_WEIGHT = 2.5F;

    /**
     * Constructs a first-hand knowledge entry recording that this villager's courtship attempt
     * failed with the given reason.
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

        return KnowledgeEntry.fromDirectObservation(originObservationId, ObservationType.SOCIAL,
                currentTick, currentTick, partnerId, metadata, SELF_FAILURE_WEIGHT, null);
    }

    /**
     * Materializes the metadata map the projector reads to render the seed phrase.
     * <p>
     * Written with the same {@link ObservationMetadataKeys} constants a perceived observation is
     * stored under, so an entry recorded here is indistinguishable in shape from one the
     * perception lane wrote and needs no reader of its own.
     */
    public static Map<String, String> buildMetadata(@Nonnull UUID actorId, @Nonnull String reason) {
        Map<String, String> metadata = new HashMap<>(4);
        metadata.put(ObservationMetadataKeys.EVENT_TYPE, WorldEventType.COURTSHIP_DATE_COMPLETED.name());
        metadata.put(ObservationMetadataKeys.ACTOR_ID, actorId.toString());
        metadata.put(ObservationMetadataKeys.OUTCOME, EventOutcome.FAILURE.name());
        metadata.put(ObservationMetadataKeys.REASON, reason);
        return Map.copyOf(metadata);
    }

}
