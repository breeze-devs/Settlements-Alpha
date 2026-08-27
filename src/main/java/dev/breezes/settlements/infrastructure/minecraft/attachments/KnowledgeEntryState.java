package dev.breezes.settlements.infrastructure.minecraft.attachments;

import lombok.Builder;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

/**
 * Flat, serialization-friendly record carrying the persisted fields of
 * {@link dev.breezes.settlements.domain.ai.knowledge.KnowledgeEntry}.
 * Used exclusively by the NBT attachment codec.
 * <p>
 * The entry's semantic type has no field here: it is a function of the event_type metadata key,
 * so persisting it would store one fact twice and let the two copies disagree.
 */
@Builder(toBuilder = true)
public record KnowledgeEntryState(
        UUID originObservationId,
        long originTimestampTick,
        long admittedAtTick,
        @Nullable UUID relatedEntity,
        Map<String, String> metadata,
        @Nullable Long packedPos,
        float weight
) {

}
