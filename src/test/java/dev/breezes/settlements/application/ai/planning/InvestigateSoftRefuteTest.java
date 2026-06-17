package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.credibility.CredibilityStore;
import dev.breezes.settlements.domain.ai.credibility.ReputationQuery;
import dev.breezes.settlements.domain.ai.knowledge.KnowledgeEntry;
import dev.breezes.settlements.domain.ai.knowledge.KnowledgeResolution;
import dev.breezes.settlements.domain.ai.knowledge.VillagerKnowledgeStore;
import dev.breezes.settlements.domain.ai.observation.ObservationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the Phase 3 soft-refute behavior:
 * <ul>
 *   <li>Selector skips a tip that is still within its post-timeout cooldown window.</li>
 *   <li>After ATTEMPT_CAP timeouts the tip is resolved REFUTED and source credibility drops.</li>
 * </ul>
 * No Minecraft types are used. All tests follow Arrange / Act / Assert.
 */
class InvestigateSoftRefuteTest {

    private static final int ATTEMPT_CAP = 3;
    private static final long COOLDOWN_TICKS = 20L * 90L; // 90 seconds at 20 tps

    private VillagerKnowledgeStore store;
    private UUID observerUUID;
    private final ReputationQuery neutralReputation = (observer, source) -> 1.0f;

    @BeforeEach
    void setUp() {
        this.store = new VillagerKnowledgeStore();
        this.observerUUID = UUID.randomUUID();
    }

    // -------------------------------------------------------------------------
    // Cooldown filtering
    // -------------------------------------------------------------------------

    @Test
    void select_skipsTipOnCooldown() {
        // Arrange: a pending tip that just had a nav timeout
        long nowTick = 1000L;
        UUID sourceId = UUID.randomUUID();
        UUID tipId = UUID.randomUUID();
        KnowledgeEntry tip = hearsayEntry(tipId, sourceId, 2.0f);
        this.store.admit(tip);

        // Simulate one nav timeout: the tip's nextEligibleTick is now > nowTick
        tip.recordNavigationTimeout(nowTick, COOLDOWN_TICKS);

        // Act: select at the same tick — still inside the cooldown window
        KnowledgeEntry result = InvestigateTipSelector.select(this.store, this.observerUUID, this.neutralReputation, nowTick);

        // Assert: selector skips the cooled-down tip
        assertNull(result, "Tip on cooldown must not be selected");
    }

    @Test
    void select_returnsTipAfterCooldownExpires() {
        // Arrange: a pending tip whose cooldown has already expired
        long cooldownStartTick = 1000L;
        UUID sourceId = UUID.randomUUID();
        UUID tipId = UUID.randomUUID();
        KnowledgeEntry tip = hearsayEntry(tipId, sourceId, 2.0f);
        this.store.admit(tip);

        tip.recordNavigationTimeout(cooldownStartTick, COOLDOWN_TICKS);

        // Act: select after the cooldown window has passed
        long afterCooldown = cooldownStartTick + COOLDOWN_TICKS + 1;
        KnowledgeEntry result = InvestigateTipSelector.select(this.store, this.observerUUID, this.neutralReputation, afterCooldown);

        // Assert: tip is now eligible again
        assertNotNull(result, "Tip should be selectable once cooldown expires");
        assertEquals(tipId, result.getOriginObservationId());
    }

    @Test
    void select_prefersCooldownFreeEntryOverHigherWeightCooledEntry() {
        // Arrange: one high-weight cooled tip and one low-weight eligible tip
        long nowTick = 5000L;
        UUID source = UUID.randomUUID();
        UUID cooledTipId = UUID.randomUUID();
        UUID eligibleTipId = UUID.randomUUID();

        KnowledgeEntry cooledTip = hearsayEntry(cooledTipId, source, 10.0f);
        KnowledgeEntry eligibleTip = hearsayEntry(eligibleTipId, source, 1.0f);
        this.store.admit(cooledTip);
        this.store.admit(eligibleTip);

        cooledTip.recordNavigationTimeout(nowTick, COOLDOWN_TICKS);

        // Act
        KnowledgeEntry result = InvestigateTipSelector.select(this.store, this.observerUUID, this.neutralReputation, nowTick);

        // Assert: cooled tip must be skipped even though raw weight is higher
        assertNotNull(result);
        assertEquals(eligibleTipId, result.getOriginObservationId(),
                "Selector must skip cooled tip even when it has higher raw weight");
    }

    // -------------------------------------------------------------------------
    // Soft-refute after ATTEMPT_CAP
    // -------------------------------------------------------------------------

    @Test
    void recordNavigationTimeout_incrementsAttemptCount() {
        // Arrange
        KnowledgeEntry tip = hearsayEntry(UUID.randomUUID(), UUID.randomUUID(), 2.0f);
        long nowTick = 100L;

        // Act
        tip.recordNavigationTimeout(nowTick, COOLDOWN_TICKS);
        tip.recordNavigationTimeout(nowTick + COOLDOWN_TICKS + 1, COOLDOWN_TICKS);

        // Assert
        assertEquals(2, tip.getInvestigationAttempts());
    }

    @Test
    void credibilityDropsWhenSourceReachesAttemptCap() {
        // Arrange: track how refutations affect credibility
        UUID sourceId = UUID.randomUUID();
        UUID observerId = UUID.randomUUID();
        CredibilityStore credibilityStore = new CredibilityStore();

        float scoreBeforeRefute = credibilityStore.getScore(sourceId);

        // Act: record a refutation (as InvestigateBehavior would do on soft-refute)
        long verifiedAtTick = 10000L;
        long originTick = 1000L;
        credibilityStore.recordRefutation(sourceId, verifiedAtTick, originTick);

        // Assert: score decreases from neutral
        float scoreAfterRefute = credibilityStore.getScore(sourceId);
        assertTrue(scoreAfterRefute < scoreBeforeRefute,
                "Source credibility should drop after a refutation");
    }

    @Test
    void tipBecomesIneligibleAfterAttemptCapResolvesRefuted() {
        // Arrange: simulate the state a tip would be in after soft-refute
        UUID sourceId = UUID.randomUUID();
        UUID tipId = UUID.randomUUID();
        KnowledgeEntry tip = hearsayEntry(tipId, sourceId, 3.0f);
        this.store.admit(tip);

        // Simulate ATTEMPT_CAP nav timeouts
        long tick = 0L;
        for (int i = 0; i < ATTEMPT_CAP; i++) {
            tip.recordNavigationTimeout(tick, COOLDOWN_TICKS);
            tick += COOLDOWN_TICKS + 1;
        }
        // Soft-refute would set the resolution to REFUTED
        tip.resolve(KnowledgeResolution.REFUTED);

        // Act: try to select after soft-refute (well after all cooldowns)
        KnowledgeEntry result = InvestigateTipSelector.select(this.store, this.observerUUID, this.neutralReputation, tick + 1_000_000L);

        // Assert: resolved tip is never selected (isPendingTip = false for REFUTED)
        assertNull(result, "Soft-refuted tip must not be selected by the selector");
    }

    @Test
    void tipRemainsEligibleBelowAttemptCap() {
        // Arrange
        UUID sourceId = UUID.randomUUID();
        UUID tipId = UUID.randomUUID();
        KnowledgeEntry tip = hearsayEntry(tipId, sourceId, 2.0f);
        this.store.admit(tip);

        // Simulate ATTEMPT_CAP - 1 timeouts (still below cap, no refute yet)
        long tick = 0L;
        for (int i = 0; i < ATTEMPT_CAP - 1; i++) {
            tip.recordNavigationTimeout(tick, COOLDOWN_TICKS);
            tick += COOLDOWN_TICKS + 1;
        }

        // Act: select after all cooldowns expired but below the cap
        KnowledgeEntry result = InvestigateTipSelector.select(this.store, this.observerUUID, this.neutralReputation, tick);

        // Assert: tip is still eligible (not yet refuted, cooldown has expired)
        assertNotNull(result, "Tip below attempt cap should become eligible again after cooldown");
        assertEquals(tipId, result.getOriginObservationId());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static KnowledgeEntry hearsayEntry(UUID originId, UUID sourceId, float weight) {
        KnowledgeEntry direct = KnowledgeEntry.fromDirectObservation(
                originId, "test tip", ObservationType.RESOURCE,
                100L, 100L, null, Map.of(), weight);
        return KnowledgeEntry.fromHearsay(direct, sourceId, 200L, weight);
    }

}
