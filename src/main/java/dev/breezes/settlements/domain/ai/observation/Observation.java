package dev.breezes.settlements.domain.ai.observation;

import dev.breezes.settlements.domain.ai.worldevent.EventOutcome;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import lombok.Builder;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

/**
 * A single discrete event perceived by a villager, queued in the {@link ObservationBuffer}
 * before the admission gate determines whether it is promoted to episodic memory.
 * <p>
 * {@code id} is a stable identity for this observation, carried onto the promoted entry as its
 * originObservationId. Independent witnesses of one occurrence derive the same id, so a store
 * already holding that occurrence recognizes the second arrival as the fact it has rather than a
 * second fact.
 * <p>
 * Event-derived details stay in typed fields while the observation is buffered so the
 * hot perception path does not allocate a metadata map for observations that never promote.
 */
@Builder
public record Observation(
        UUID id,
        long timestampTick,
        ObservationType type,
        WorldEventType eventType,
        String content,
        float baseImportance,
        @Nullable UUID relatedEntity,
        @Nullable UUID actorId,
        @Nullable UUID registryId,
        @Nullable String eventMetadata,
        @Nullable EventOutcome outcome,
        @Nullable String reason,
        @Nullable String detail,
        @Nullable Map<String, String> detailFields,
        double posX,
        double posY,
        double posZ
) {

}
