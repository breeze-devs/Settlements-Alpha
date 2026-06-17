package dev.breezes.settlements.domain.ai.observation;

import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import lombok.Builder;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A single discrete event perceived by a villager, queued in the {@link ObservationBuffer}
 * before the importance gate determines whether it is promoted to episodic memory.
 * <p>
 * {@code id} is a stable identity for this observation used as the deduplication key ({@code originObservationId}).
 * Observations derived from the same world-event carry the same id on both the initiator and the receiver so the
 * store can reject news that has already been seen regardless of the gossip path it traveled.
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
        double posX,
        double posY,
        double posZ
) {

}
