package dev.breezes.settlements.domain.ai.observation;

import lombok.Builder;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

/**
 * A single discrete event perceived by a villager, queued in the {@link ObservationBuffer}
 * before the importance gate determines whether it is promoted to episodic memory.
 * <p>
 * {@code metadata} is a free-form string map for observation-type-specific details —
 * keep values small and serialization-safe.
 */
@Builder
public record Observation(
        long timestampTick,
        ObservationType type,
        String content,
        float baseImportance,
        @Nullable UUID relatedEntity,
        Map<String, String> metadata
) {

}
