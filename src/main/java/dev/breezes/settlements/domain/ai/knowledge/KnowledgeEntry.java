package dev.breezes.settlements.domain.ai.knowledge;

import dev.breezes.settlements.domain.ai.observation.ObservationType;
import lombok.Builder;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

/**
 * A single promoted fact in a villager's episodic knowledge store
 * <p>
 * Every entry carries both the core observation data and full provenance so the
 * {@link VillagerKnowledgeStore} can deduplicate and weight hearsay entries without
 * losing the chain of custody.
 */
@Builder
@Getter
public final class KnowledgeEntry {

    /**
     * The maximum number of gossip hops before an entry can no longer be re-shared
     * <p>
     * Keeps the settlement from becoming a perfect-information network
     */
    public static final int MAX_HOP_COUNT = 3;


    /**
     * Stable identity of the originating observation; can be used to deduplicate
     */
    private final UUID originObservationId;

    /**
     * Semantic type of the original observation.
     */
    private final ObservationType type;

    /**
     * Game tick at which this entry was first observed (at the origin, not at this villager).
     * Used to compute staleness in weight calculations.
     */
    private final long originTimestampTick;

    /**
     * Game tick at which this entry was admitted into this villager's store.
     * For first-hand entries this equals {@link #originTimestampTick}; for hearsay entries it is later
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
     * UUID of the villager who directly shared this entry during a gossip exchange.
     * Null for first-hand observations (hop == 0).
     */
    @Nullable
    private final UUID source;

    /**
     * Gossip hop count. 0 = directly observed; 1 = heard once; 2 = heard twice; etc.
     */
    private final int hop;

    /**
     * Composite weight at admission time
     * <p>
     * Mutable so {@link #corroborate(float)} can bump it when independent sources confirm the same fact
     */
    private float weight;

    /**
     * Weight assigned at construction time; never mutated.
     * <p>
     * Stored separately so the corroboration cap is always 2× the original value rather than 2×
     * whatever the current weight happens to be, which would allow unbounded growth across many
     * corroboration events.
     */
    private final float originalWeight;

    /**
     * Number of independent sources that have corroborated this fact
     * <p>
     * Starts at 0; incremented each time a different independent source shares the same
     * origin-id to the receiver.
     */
    private int corroborationCount;


    /**
     * Records that an independent source corroborated this fact and adjusts its weight upward
     * <p>
     * Weight is bumped by the caller-supplied absolute delta per corroboration, capped at 2×
     * the original weight to avoid unbounded inflation. The bump is intentionally modest:
     * corroboration signals additional confidence, not a proportional increase in magnitude.
     *
     * @param weightBump absolute weight delta applied to the existing entry's weight
     */
    public void corroborate(float weightBump) {
        this.corroborationCount++;
        // Cap against the original weight, not the current weight: capping against the current
        // value would let the ceiling rise with every corroboration, making the cap a no-op.
        this.weight = Math.min(this.weight + weightBump, this.originalWeight * 2.0f);
    }

    /**
     * Recomputes the composite weight from stored, persisted fields — the same formula
     * {@link #corroborate} applies incrementally, but as a pure function of the two values that
     * are actually persisted ({@code originalWeight}, {@code corroborationCount}). Used on load
     * so {@code weight} itself does not need to be persisted.
     *
     * @param originalWeight     weight assigned at construction time (persisted)
     * @param corroborationCount number of independent corroborations applied (persisted)
     * @param corroborationBump  absolute weight delta applied per corroboration
     *                           ({@link VillagerKnowledgeStore#CORROBORATION_BUMP})
     */
    public static float recomputeWeight(float originalWeight, int corroborationCount, float corroborationBump) {
        return Math.min(originalWeight + corroborationBump * corroborationCount, originalWeight * 2.0f);
    }

    /**
     * Whether this entry can be re-shared during a gossip exchange
     * <p>
     * Entries at or above the hop cap are retained locally but never forwarded
     */
    public boolean isShareable() {
        return this.hop < MAX_HOP_COUNT;
    }

    public boolean isHearsay() {
        return this.hop > 0;
    }

    /**
     * Builds a first-hand entry (direct observation, no hearsay provenance)
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
                .source(null)
                .hop(0)
                .weight(weight)
                .originalWeight(weight)
                .corroborationCount(0)
                .build();
    }

    /**
     * Builds a hearsay entry from a shared source entry, incrementing the hop count and
     * applying the weight reduction factor computed by the gossip engine.
     *
     * @param sourceEntry    the entry being shared by the gossiper
     * @param sourceId       UUID of the villager who shared it
     * @param admittedAtTick tick at which the receiver admits this entry
     * @param adjustedWeight weight after staleness × hop × CHA reduction
     */
    public static KnowledgeEntry fromHearsay(KnowledgeEntry sourceEntry,
                                             UUID sourceId,
                                             long admittedAtTick,
                                             float adjustedWeight) {
        return KnowledgeEntry.builder()
                .originObservationId(sourceEntry.getOriginObservationId())
                .type(sourceEntry.getType())
                .originTimestampTick(sourceEntry.getOriginTimestampTick())
                .admittedAtTick(admittedAtTick)
                .relatedEntity(sourceEntry.getRelatedEntity())
                .metadata(sourceEntry.getMetadata())
                .packedPos(sourceEntry.getPackedPos())
                .source(sourceId)
                .hop(sourceEntry.getHop() + 1)
                .weight(adjustedWeight)
                .originalWeight(adjustedWeight)
                .corroborationCount(0)
                .build();
    }


}
