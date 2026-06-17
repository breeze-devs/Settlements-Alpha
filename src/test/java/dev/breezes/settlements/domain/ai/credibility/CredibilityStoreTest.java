package dev.breezes.settlements.domain.ai.credibility;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link CredibilityStore}.
 * No Minecraft types are used. All tests follow Arrange / Act / Assert.
 */
class CredibilityStoreTest {

    private CredibilityStore store;
    private UUID sourceId;

    @BeforeEach
    void setUp() {
        this.store = new CredibilityStore();
        this.sourceId = UUID.randomUUID();
    }

    // -------------------------------------------------------------------------
    // Default / unknown source
    // -------------------------------------------------------------------------

    @Test
    void getMultiplier_returnsOneForUnknownSource() {
        // Arrange: no record for source
        // Act
        float multiplier = this.store.getMultiplier(this.sourceId);
        // Assert: unknown source == neutral == no penalty or bonus
        assertEquals(1.0f, multiplier, 0.001f);
    }

    @Test
    void getScore_returnsNeutralScoreForUnknownSource() {
        // Arrange: no record
        // Act
        float score = this.store.getScore(this.sourceId);
        // Assert
        assertEquals(CredibilityStore.NEUTRAL_SCORE, score, 0.001f);
    }

    // -------------------------------------------------------------------------
    // Confirmation bumps score above neutral
    // -------------------------------------------------------------------------

    @Test
    void recordConfirmation_raisesScoreAboveNeutral() {
        // Arrange
        long tick = 1000L;

        // Act
        this.store.recordConfirmation(this.sourceId, tick, tick);

        // Assert: score should now be above the neutral midpoint
        float score = this.store.getScore(this.sourceId);
        assertTrue(score > CredibilityStore.NEUTRAL_SCORE,
                "Expected score > neutral after one confirmation, got " + score);
    }

    @Test
    void getMultiplier_aboveOneAfterConfirmation() {
        // Arrange
        long tick = 1000L;

        // Act
        this.store.recordConfirmation(this.sourceId, tick, tick);

        // Assert
        float multiplier = this.store.getMultiplier(this.sourceId);
        assertTrue(multiplier > 1.0f,
                "Expected multiplier > 1.0 after confirmation, got " + multiplier);
    }

    // -------------------------------------------------------------------------
    // Refutation pushes score below neutral — but LESS than confirmation raises
    // -------------------------------------------------------------------------

    @Test
    void recordRefutation_lowersScoreBelowNeutral() {
        // Arrange
        long tick = 1000L;

        // Act
        this.store.recordRefutation(this.sourceId, tick, tick);

        // Assert: score drops below neutral
        float score = this.store.getScore(this.sourceId);
        assertTrue(score < CredibilityStore.NEUTRAL_SCORE,
                "Expected score < neutral after refutation, got " + score);
    }

    @Test
    void confirmationDeltaLargerThanRefutationDelta() {
        // Arrange: start from neutral, apply equal-staleness signals
        long tick = 1000L;
        CredibilityStore refutedStore = new CredibilityStore();
        CredibilityStore confirmedStore = new CredibilityStore();

        // Act
        refutedStore.recordRefutation(this.sourceId, tick, tick);
        confirmedStore.recordConfirmation(this.sourceId, tick, tick);

        // Assert: the confirmation delta moves the score farther from neutral than the refutation
        float refuteDeviation = CredibilityStore.NEUTRAL_SCORE - refutedStore.getScore(this.sourceId);
        float confirmDeviation = confirmedStore.getScore(this.sourceId) - CredibilityStore.NEUTRAL_SCORE;
        assertTrue(confirmDeviation > refuteDeviation,
                "Confirmation delta should exceed refutation delta (asymmetry), confirm=" + confirmDeviation + " refute=" + refuteDeviation);
    }

    // -------------------------------------------------------------------------
    // Staleness scaling
    // -------------------------------------------------------------------------

    @Test
    void fresherTip_producesSmallerPenaltyForStaleTip() {
        // Arrange: same origin and source but different verification timing
        // "stale" = verified very long after origin; "fresh" = verified immediately
        CredibilityStore staleStore = new CredibilityStore();
        CredibilityStore freshStore = new CredibilityStore();

        UUID source1 = UUID.randomUUID();
        UUID source2 = UUID.randomUUID();

        long originTick = 0L;
        long freshVerificationTick = 0L;            // verified immediately (max staleness scale)
        long staleVerificationTick = 72_000L;       // ~1 game day later (much lower scale)

        // Act
        freshStore.recordRefutation(source1, freshVerificationTick, originTick);
        staleStore.recordRefutation(source2, staleVerificationTick, originTick);

        // Assert: fresh refutation results in a bigger penalty (score further from neutral)
        float freshDeviation = CredibilityStore.NEUTRAL_SCORE - freshStore.getScore(source1);
        float staleDeviation = CredibilityStore.NEUTRAL_SCORE - staleStore.getScore(source2);
        assertTrue(freshDeviation > staleDeviation,
                "Fresh refutation should produce a larger penalty than stale refutation, fresh=" + freshDeviation + " stale=" + staleDeviation);
    }

    // -------------------------------------------------------------------------
    // Decay toward neutral
    // -------------------------------------------------------------------------

    @Test
    void tickDecay_pullsScoreTowardNeutral_afterConfirmation() {
        // Arrange
        long tick = 1000L;
        this.store.recordConfirmation(this.sourceId, tick, tick);
        float scoreAfterConfirm = this.store.getScore(this.sourceId);
        assertTrue(scoreAfterConfirm > CredibilityStore.NEUTRAL_SCORE);

        // Act: apply substantial decay (simulate many ticks)
        this.store.tickDecay(1_000_000L);

        // Assert: score has moved closer to neutral but clamped above MIN_SCORE
        float scoreAfterDecay = this.store.getScore(this.sourceId);
        assertTrue(scoreAfterDecay < scoreAfterConfirm,
                "Score should move toward neutral after decay, before=" + scoreAfterConfirm + " after=" + scoreAfterDecay);
        assertTrue(scoreAfterDecay >= CredibilityStore.MIN_SCORE,
                "Score must not drop below MIN_SCORE");
    }

    @Test
    void tickDecay_pullsScoreTowardNeutral_afterRefutation() {
        // Arrange
        long tick = 1000L;
        this.store.recordRefutation(this.sourceId, tick, tick);
        float scoreAfterRefute = this.store.getScore(this.sourceId);
        assertTrue(scoreAfterRefute < CredibilityStore.NEUTRAL_SCORE);

        // Act
        this.store.tickDecay(1_000_000L);

        // Assert: score has moved closer to neutral (increased toward 0.5)
        float scoreAfterDecay = this.store.getScore(this.sourceId);
        assertTrue(scoreAfterDecay > scoreAfterRefute,
                "Score should move toward neutral after decay, before=" + scoreAfterRefute + " after=" + scoreAfterDecay);
        assertTrue(scoreAfterDecay <= CredibilityStore.MAX_SCORE,
                "Score must not exceed MAX_SCORE");
    }

    @Test
    void tickDecay_usesConfiguredDecayRate() {
        // Arrange
        CredibilityStore fastDecayStore = new CredibilityStore(0.5f);
        long tick = 1000L;
        fastDecayStore.recordConfirmation(this.sourceId, tick, tick);
        float scoreAfterConfirm = fastDecayStore.getScore(this.sourceId);

        // Act
        fastDecayStore.tickDecay(1L);

        // Assert
        float expected = CredibilityStore.NEUTRAL_SCORE
                + ((scoreAfterConfirm - CredibilityStore.NEUTRAL_SCORE) * 0.5f);
        assertEquals(0.5f, fastDecayStore.decayPerTick(), 0.001f);
        assertEquals(expected, fastDecayStore.getScore(this.sourceId), 0.001f);
    }

    // -------------------------------------------------------------------------
    // Score clamping
    // -------------------------------------------------------------------------

    @Test
    void score_clampedAtMaxAfterManyConfirmations() {
        // Arrange: apply many confirmations at max staleness scale
        long tick = 0L;
        for (int i = 0; i < 100; i++) {
            this.store.recordConfirmation(this.sourceId, tick, tick);
        }

        // Act & Assert: score must not exceed MAX_SCORE
        float score = this.store.getScore(this.sourceId);
        assertTrue(score <= CredibilityStore.MAX_SCORE,
                "Score should be clamped at MAX_SCORE, got " + score);
    }

    @Test
    void score_clampedAtMinAfterManyRefutations() {
        // Arrange
        long tick = 0L;
        for (int i = 0; i < 100; i++) {
            this.store.recordRefutation(this.sourceId, tick, tick);
        }

        // Act & Assert
        float score = this.store.getScore(this.sourceId);
        assertTrue(score >= CredibilityStore.MIN_SCORE,
                "Score should be clamped at MIN_SCORE, got " + score);
    }

    // -------------------------------------------------------------------------
    // Neutral anchor — key correctness requirement
    // -------------------------------------------------------------------------

    @Test
    void getMultiplier_atExactNeutralScore_returnsOnePointZero() {
        // Arrange: a source that has a recorded tally exactly at NEUTRAL_SCORE.
        // We achieve this by recording one confirmation and then applying massive
        // decay to collapse back near neutral, then test via direct API.
        // Easier: use a fresh score directly from getScore default (neutral = 0.5) and
        // verify the multiplier via the mapping formula.
        // Strategy: the multiplier for an unknown source must be 1.0 (documented contract).
        UUID unknownId = UUID.randomUUID();

        // Act
        float multiplier = this.store.getMultiplier(unknownId);

        // Assert: neutral score (no data) must produce exactly 1.0, not 0.9.
        assertEquals(1.0f, multiplier, 0.001f,
                "Neutral / unknown source must return multiplier of 1.0, not the old 0.9 value");
    }

    @Test
    void getMultiplier_atMinScore_returnsMinMultiplier() {
        // Arrange: force score to minimum by many fresh refutations
        long tick = 0L;
        for (int i = 0; i < 100; i++) {
            this.store.recordRefutation(this.sourceId, tick, tick);
        }
        assertEquals(CredibilityStore.MIN_SCORE, this.store.getScore(this.sourceId), 0.01f);

        // Act
        float multiplier = this.store.getMultiplier(this.sourceId);

        // Assert
        assertEquals(CredibilityStore.MIN_MULTIPLIER, multiplier, 0.01f);
    }

    @Test
    void getMultiplier_atMaxScore_returnsMaxMultiplier() {
        // Arrange: force score to maximum by many fresh confirmations
        long tick = 0L;
        for (int i = 0; i < 100; i++) {
            this.store.recordConfirmation(this.sourceId, tick, tick);
        }
        assertEquals(CredibilityStore.MAX_SCORE, this.store.getScore(this.sourceId), 0.01f);

        // Act
        float multiplier = this.store.getMultiplier(this.sourceId);

        // Assert
        assertEquals(CredibilityStore.MAX_MULTIPLIER, multiplier, 0.01f);
    }

    // -------------------------------------------------------------------------
    // FIFO eviction
    // -------------------------------------------------------------------------

    @Test
    void fifoEviction_removesOldestEntryWhenFull() {
        // Arrange: fill the store to capacity
        UUID firstSource = UUID.randomUUID();
        this.store.recordConfirmation(firstSource, 100L, 100L);

        for (int i = 0; i < 49; i++) {
            this.store.recordConfirmation(UUID.randomUUID(), 100L, 100L);
        }

        // At this point the store has 50 entries (MAX_ENTRIES).
        assertEquals(50, this.store.size());

        // Act: add one more — should evict firstSource
        UUID newSource = UUID.randomUUID();
        this.store.recordConfirmation(newSource, 100L, 100L);

        // Assert: firstSource evicted (returns neutral = no record), newSource is present
        assertEquals(CredibilityStore.NEUTRAL_SCORE, this.store.getScore(firstSource), 0.001f);
        assertTrue(this.store.getScore(newSource) > CredibilityStore.NEUTRAL_SCORE,
                "Newly admitted source should have a score above neutral");
    }

    // -------------------------------------------------------------------------
    // Staleness scale helper
    // -------------------------------------------------------------------------

    @Test
    void stalenessScale_maxAtZeroAge() {
        // Arrange & Act
        float scale = CredibilityStore.stalenessScale(0L, 0L);

        // Assert: zero age → maximum scale
        assertEquals(CredibilityStore.MAX_STALENESS_SCALE, scale, 0.001f);
    }

    @Test
    void stalenessScale_decreasesWithAge() {
        // Arrange & Act
        float fresh = CredibilityStore.stalenessScale(0L, 0L);
        float stale = CredibilityStore.stalenessScale(0L, 6_000L); // one halving period

        // Assert: freshness halves at the halving constant
        assertEquals(fresh / 2f, stale, 0.01f);
    }

}
