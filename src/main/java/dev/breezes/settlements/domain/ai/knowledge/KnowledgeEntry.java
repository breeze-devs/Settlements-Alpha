package dev.breezes.settlements.domain.ai.knowledge;

import dev.breezes.settlements.domain.ai.observation.ObservationType;
import lombok.Builder;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

/**
 * A single promoted fact in a villager's episodic knowledge store
 * <p>
 * Every entry carries both the core observation data and full provenance so the
 * {@link VillagerKnowledgeStore} can deduplicate, weight, and eventually resolve
 * hearsay entries to CONFIRMED or REFUTED without losing the chain of custody.
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
     * Human-readable description of the fact
     */
    private final String content;

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
     * Lifecycle resolution state. Only meaningful for hearsay entries awaiting Investigation.
     * Null means no verification is required (first-hand) or not yet attempted.
     */
    @Nullable
    private KnowledgeResolution resolution;

    /**
     * Number of independent sources that have corroborated this fact
     * <p>
     * Starts at 0; incremented each time a different independent source shares the same
     * origin-id to the receiver.
     */
    private int corroborationCount;

    /**
     * How many times Investigate attempted to navigate to this tip but timed out without
     * reaching the location.
     * Persisted so the cap survives server restarts and the same tip cannot be retried
     * indefinitely across sessions.
     */
    private int investigationAttempts;

    /**
     * Game-tick before which this tip is ineligible for selection by
     * {@link dev.breezes.settlements.application.ai.planning.InvestigateTipSelector}.
     * Set to {@code now + cooldown} after each nav-timeout so the villager backs off
     * before retrying. 0 means "always eligible".
     */
    private long nextEligibleTick;


    /**
     * Advances the resolution state once investigation confirms or refutes the tip
     */
    public void resolve(@Nonnull KnowledgeResolution newResolution) {
        this.resolution = newResolution;
    }

    /**
     * Records a failed navigation attempt and imposes a cooldown before the next retry.
     * <p>
     * The nav timeout means the location was unreachable at this tick; backing off prevents
     * the planner from burning an Investigate scout slot on the same dead-end every morning.
     *
     * @param nowTick       current game tick
     * @param cooldownTicks how many ticks to wait before this tip becomes eligible again
     */
    public void recordNavigationTimeout(long nowTick, long cooldownTicks) {
        this.investigationAttempts++;
        this.nextEligibleTick = nowTick + cooldownTicks;
    }

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
                                                       String content,
                                                       ObservationType type,
                                                       long originTimestampTick,
                                                       long admittedAtTick,
                                                       @Nullable UUID relatedEntity,
                                                       Map<String, String> metadata,
                                                       float weight) {
        return KnowledgeEntry.builder()
                .originObservationId(originObservationId)
                .content(content)
                .type(type)
                .originTimestampTick(originTimestampTick)
                .admittedAtTick(admittedAtTick)
                .relatedEntity(relatedEntity)
                .metadata(metadata)
                .source(null)
                .hop(0)
                .weight(weight)
                .originalWeight(weight)
                .resolution(null)
                .corroborationCount(0)
                .investigationAttempts(0)
                .nextEligibleTick(0L)
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
                .content(sourceEntry.getContent())
                .type(sourceEntry.getType())
                .originTimestampTick(sourceEntry.getOriginTimestampTick())
                .admittedAtTick(admittedAtTick)
                .relatedEntity(sourceEntry.getRelatedEntity())
                .metadata(sourceEntry.getMetadata())
                .source(sourceId)
                .hop(sourceEntry.getHop() + 1)
                .weight(adjustedWeight)
                .originalWeight(adjustedWeight)
                .resolution(KnowledgeResolution.UNRESOLVED)
                .corroborationCount(0)
                .investigationAttempts(0)
                .nextEligibleTick(0L)
                .build();
    }


}
