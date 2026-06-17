package dev.breezes.settlements.domain.ai.credibility;

import lombok.CustomLog;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-villager credibility tally for gossip sources.
 * <p>
 * Credibility is asymmetric and decaying:
 * <ul>
 *   <li><b>Confirmations</b> are weighted more heavily than refutations because
 *       a refuted tip may simply reflect world churn (someone else harvested the
 *       melons between the tip being shared and the Investigate reaching the site).
 *       Punishing honest sources for stale-but-accurate tips would be unfair.</li>
 *   <li><b>Staleness-at-verification</b> scales the penalty: a tip verified within
 *       minutes warrants a stronger signal than one confirmed or refuted the next morning,
 *       when the world could have changed in any direction.</li>
 *   <li><b>Decay</b> pulls each tally toward the neutral midpoint over time so a
 *       single bad tip does not permanently blacklist a source and a run of good tips
 *       does not create a blind-trust oracle.</li>
 * </ul>
 * TODO: performance -- consider FastUtil to eliminate boxed Float objects
 */
@CustomLog
public final class CredibilityStore {

    public static final float MIN_SCORE = 0.0f;
    public static final float MAX_SCORE = 1.0f;
    public static final float NEUTRAL_SCORE = 0.5f;

    /**
     * Multiplier bounds that the score maps onto. A source at MAX_SCORE applies
     * MAX_MULTIPLIER to hearsay weight; at MIN_SCORE it applies MIN_MULTIPLIER
     */
    public static final float MIN_MULTIPLIER = 0.3f;
    public static final float MAX_MULTIPLIER = 1.5f;

    /**
     * How much a confirmed tip moves the credibility score toward MAX_SCORE
     */
    public static final float CONFIRM_DELTA = 0.12f;

    /**
     * How much a refuted tip moves the credibility score toward MIN_SCORE
     * Smaller than CONFIRM_DELTA because refutation is confounded by world churn
     */
    public static final float REFUTE_DELTA = 0.04f;

    /**
     * Maximum staleness scaling factor — at zero staleness (verified instantly) the
     * delta is multiplied by this. At high staleness the multiplier approaches zero,
     * so very stale tips barely move credibility either direction
     */
    public static final float MAX_STALENESS_SCALE = 1.5f;

    /**
     * Halving time for staleness scaling in ticks
     */
    private static final double STALENESS_HALVING_TICKS = 6_000.0;

    public static final float DEFAULT_DECAY_PER_TICK = (float) (1.0 - Math.pow(0.5, 1.0 / 72_000.0));

    /**
     * Maximum entries kept (FIFO eviction)
     */
    private static final int MAX_ENTRIES = 50;

    /**
     * Credibility scores keyed by source villager UUID, insertion-ordered
     */
    private final Map<UUID, Float> scores;
    private final float decayPerTick;

    public CredibilityStore() {
        this(DEFAULT_DECAY_PER_TICK);
    }

    public CredibilityStore(float decayPerTick) {
        if (decayPerTick < 0.0f || decayPerTick >= 1.0f) {
            throw new IllegalArgumentException("decayPerTick must be >= 0 and < 1");
        }
        this.decayPerTick = decayPerTick;
        this.scores = new LinkedHashMap<>(MAX_ENTRIES, 0.75f, false) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<UUID, Float> eldest) {
                boolean shouldRemove = this.size() > MAX_ENTRIES;
                if (shouldRemove) {
                    log.debug("CredibilityStore: evicted oldest entry {}", eldest.getKey());
                }
                return shouldRemove;
            }
        };
    }


    /**
     * Records that a tip from {@code sourceId} was CONFIRMED
     *
     * @param sourceId       UUID of the villager whose tip was investigated
     * @param verifiedAtTick game tick at which investigation completed
     * @param tipOriginTick  game tick at which the original fact was observed
     */
    public void recordConfirmation(@Nonnull UUID sourceId, long verifiedAtTick, long tipOriginTick) {
        float stalenessScale = stalenessScale(tipOriginTick, verifiedAtTick);
        float delta = CONFIRM_DELTA * stalenessScale;
        this.adjust(sourceId, delta);
        log.debug("Credibility CONFIRMED for source={}: delta={} staleness={}", sourceId, delta, stalenessScale);
    }

    /**
     * Records that a tip from {@code sourceId} was REFUTED
     *
     * @param sourceId       UUID of the villager whose tip was investigated
     * @param verifiedAtTick game tick at which investigation completed
     * @param tipOriginTick  game tick at which the original fact was observed
     */
    public void recordRefutation(@Nonnull UUID sourceId, long verifiedAtTick, long tipOriginTick) {
        float stalenessScale = stalenessScale(tipOriginTick, verifiedAtTick);
        float delta = -(REFUTE_DELTA * stalenessScale);
        this.adjust(sourceId, delta);
        log.debug("Credibility REFUTED for source={}: delta={} staleness={}", sourceId, delta, stalenessScale);
    }

    /**
     * Applies time-based decay to all scores, pulling each toward the neutral midpoint
     *
     * @param elapsedTicks number of ticks since the last decay call
     */
    public void tickDecay(long elapsedTicks) {
        if (this.scores.isEmpty()) {
            return;
        }

        // Decay factor is how much of the deviation from neutral remains after elapsedTicks.
        double decayFactor = Math.pow(1.0 - this.decayPerTick, elapsedTicks);
        this.scores.replaceAll((id, score) -> {
            float deviation = score - NEUTRAL_SCORE;
            // Decay the deviation; score converges toward NEUTRAL_SCORE over time.
            return NEUTRAL_SCORE + (float) (deviation * decayFactor);
        });
    }

    /**
     * Returns the credibility multiplier for the given source, defaulting to 1.0 for unknown sources
     */
    public float getMultiplier(@Nonnull UUID sourceId) {
        float score = this.scores.getOrDefault(sourceId, NEUTRAL_SCORE);
        if (score <= NEUTRAL_SCORE) {
            // Lower segment: score 0.0 → 0.3, score 0.5 → 1.0
            float t = (score - MIN_SCORE) / (NEUTRAL_SCORE - MIN_SCORE);
            return MIN_MULTIPLIER + t * (1.0f - MIN_MULTIPLIER);
        } else {
            // Upper segment: score 0.5 → 1.0, score 1.0 → 1.5
            float t = (score - NEUTRAL_SCORE) / (MAX_SCORE - NEUTRAL_SCORE);
            return 1.0f + t * (MAX_MULTIPLIER - 1.0f);
        }
    }

    /**
     * Returns the raw credibility score for the given source
     */
    public float getScore(UUID sourceId) {
        return this.scores.getOrDefault(sourceId, NEUTRAL_SCORE);
    }

    public int size() {
        return this.scores.size();
    }

    public float decayPerTick() {
        return this.decayPerTick;
    }

    /**
     * Returns a defensive snapshot of all source→score pairs for NBT serialization.
     * Called from the persistence layer; the returned map is a copy and must not be
     * mutated by callers.
     */
    public Map<UUID, Float> snapshotScores() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(this.scores));
    }

    /**
     * Overwrites the internal score map from a deserialized snapshot, replacing any
     * previously held entries. Called from the NBT load path; eviction is silently skipped
     * because the snapshot was already bounded at save time.
     *
     * @param snapshot source → score pairs deserialized from NBT
     */
    public void restoreScores(Map<UUID, Float> snapshot) {
        this.scores.clear();
        this.scores.putAll(snapshot);
    }


    private void adjust(UUID sourceId, float delta) {
        float current = this.scores.getOrDefault(sourceId, NEUTRAL_SCORE);
        float updated = Math.clamp(current + delta, MIN_SCORE, MAX_SCORE);
        this.scores.put(sourceId, updated);
    }

    /**
     * Staleness scaling factor: 1.5 when verified immediately (fresh = strong signal),
     * decaying exponentially toward 0 as the gap between origin and verification grows
     */
    static float stalenessScale(long tipOriginTick, long verifiedAtTick) {
        long ageTicks = Math.max(0L, verifiedAtTick - tipOriginTick);
        double freshness = Math.pow(0.5, ageTicks / STALENESS_HALVING_TICKS);
        return (float) (MAX_STALENESS_SCALE * freshness);
    }

}
