package dev.breezes.settlements.domain.ai.knowledge;

import dev.breezes.settlements.domain.ai.observation.ObservationType;
import lombok.Builder;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

/**
 * A single promoted fact in a villager's episodic knowledge store.
 * <p>
 * Every entry is first-hand — it records something this villager perceived itself — and is
 * identified by the observation it came from, so two perceptions of one occurrence are the
 * same entry rather than two.
 * <p>
 * TODO: redesign the entry shape — identity, salience, and the metadata vocabulary are flattened
 *  into one record here, and the fields a reinstated corroboration model needs are undecided.
 */
@Builder
@Getter
public final class KnowledgeEntry {

    /**
     * Stable identity of the originating observation; can be used to deduplicate
     */
    private final UUID originObservationId;

    /**
     * Semantic type of the original observation.
     */
    private final ObservationType type;

    /**
     * Game tick at which the originating observation occurred.
     */
    private final long originTimestampTick;

    /**
     * Game tick at which this entry was admitted into this villager's store.
     */
    private final long admittedAtTick;

    /**
     * Optional entity UUID the fact is about
     */
    @Nullable
    private final UUID relatedEntity;

    /**
     * Free-form metadata from the originating observation, kept for downstream queries.
     */
    private final Map<String, String> metadata;

    /**
     * Packed block position of the originating observation, or null when the observation
     * carried no spatial grounding (e.g. a private courtship self-failure).
     * <p>
     * Packed via {@link dev.breezes.settlements.domain.ai.memory.PackedPos#asLong} from the
     * floored observation coordinates. Nullable because absence is a real, live case, not
     * just a persistence-layer default.
     */
    @Nullable
    private final Long packedPos;

    /**
     * Salience of this fact, fixed at admission. Only the ordering between entries is meaningful;
     * the magnitude carries no unit.
     */
    private final float weight;

    /**
     * Builds an entry for an observation this villager perceived directly
     */
    public static KnowledgeEntry fromDirectObservation(UUID originObservationId,
                                                       ObservationType type,
                                                       long originTimestampTick,
                                                       long admittedAtTick,
                                                       @Nullable UUID relatedEntity,
                                                       Map<String, String> metadata,
                                                       float weight,
                                                       @Nullable Long packedPos) {
        return KnowledgeEntry.builder()
                .originObservationId(originObservationId)
                .type(type)
                .originTimestampTick(originTimestampTick)
                .admittedAtTick(admittedAtTick)
                .relatedEntity(relatedEntity)
                .metadata(metadata)
                .packedPos(packedPos)
                .weight(weight)
                .build();
    }

}
