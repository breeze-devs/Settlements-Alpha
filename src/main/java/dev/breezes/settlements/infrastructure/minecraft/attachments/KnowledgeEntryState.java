package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.domain.ai.knowledge.KnowledgeResolution;
import dev.breezes.settlements.domain.ai.observation.ObservationType;
import lombok.Builder;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

/**
 * Flat, serialization-friendly record mirroring the fields of {@link dev.breezes.settlements.domain.ai.knowledge.KnowledgeEntry}.
 * Used exclusively by the NBT attachment codec
 */
@Builder
public record KnowledgeEntryState(
        UUID originObservationId,
        String content,
        ObservationType type,
        long originTimestampTick,
        long admittedAtTick,
        @Nullable UUID relatedEntity,
        Map<String, String> metadata,
        @Nullable UUID source,
        int hop,
        float weight,
        float originalWeight,
        @Nullable KnowledgeResolution resolution,
        int corroborationCount,
        int investigationAttempts,
        long nextEligibleTick
) {

}
