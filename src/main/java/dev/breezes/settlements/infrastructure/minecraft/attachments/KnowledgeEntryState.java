package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.domain.ai.knowledge.KnowledgeResolution;
import lombok.Builder;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

/**
 * Flat, serialization-friendly record mirroring the fields of {@link dev.breezes.settlements.domain.ai.knowledge.KnowledgeEntry}.
 * Used exclusively by the NBT attachment codec.
 */
@Builder(toBuilder = true)
public record KnowledgeEntryState(
        UUID originObservationId,
        long originTimestampTick,
        long admittedAtTick,
        @Nullable UUID relatedEntity,
        Map<String, String> metadata,
        @Nullable Long packedPos,
        @Nullable UUID source,
        int hop,
        float originalWeight,
        @Nullable KnowledgeResolution resolution,
        int corroborationCount,
        int investigationAttempts,
        long nextEligibleTick
) {

}
