package dev.breezes.settlements.application.ai.planning;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link InvestigateTipSelector}.
 * No Minecraft types are used. All tests follow Arrange / Act / Assert.
 */
class InvestigateTipSelectorTest {

    private static final long NOW_TICK = 1_000L;

    private VillagerKnowledgeStore store;
    private UUID observerUUID;

    /**
     * Neutral reputation query that always returns 1.0 (no bias).
     */
    private final ReputationQuery neutralReputation = (observer, source) -> 1.0f;

    @BeforeEach
    void setUp() {
        this.store = new VillagerKnowledgeStore();
        this.observerUUID = UUID.randomUUID();
    }

    // -------------------------------------------------------------------------
    // isPendingTip
    // -------------------------------------------------------------------------

    @Test
    void isPendingTip_trueForUnresolvedHearsay() {
        // Arrange
        KnowledgeEntry hearsay = hearsayEntry(UUID.randomUUID(), UUID.randomUUID(), 2.0f, null);

        // Act & Assert
        assertTrue(InvestigateTipSelector.isPendingTip(hearsay));
    }

    @Test
    void isPendingTip_falseForFirstHandEntry() {
        // Arrange: first-hand, not hearsay
        KnowledgeEntry direct = directEntry(UUID.randomUUID(), 2.0f);

        // Act & Assert
        assertFalse(InvestigateTipSelector.isPendingTip(direct));
    }

    @Test
    void isPendingTip_falseForResolvedHearsay() {
        // Arrange: hearsay but already CONFIRMED
        KnowledgeEntry resolved = hearsayEntry(UUID.randomUUID(), UUID.randomUUID(), 2.0f, KnowledgeResolution.CONFIRMED);

        // Act & Assert
        assertFalse(InvestigateTipSelector.isPendingTip(resolved));
    }

    @Test
    void isPendingTip_falseForRefutedHearsay() {
        // Arrange: hearsay already REFUTED
        KnowledgeEntry refuted = hearsayEntry(UUID.randomUUID(), UUID.randomUUID(), 2.0f, KnowledgeResolution.REFUTED);

        // Act & Assert
        assertFalse(InvestigateTipSelector.isPendingTip(refuted));
    }

    // -------------------------------------------------------------------------
    // select() — no entries
    // -------------------------------------------------------------------------

    @Test
    void select_returnsNullWhenNoEntries() {
        // Arrange: empty store
        // Act
        KnowledgeEntry result = InvestigateTipSelector.select(this.store, this.observerUUID, this.neutralReputation, NOW_TICK);

        // Assert
        assertNull(result);
    }

    @Test
    void select_returnsNullWhenOnlyFirstHandEntries() {
        // Arrange: store has only direct observations, no hearsay
        this.store.admit(directEntry(UUID.randomUUID(), 3.0f));

        // Act
        KnowledgeEntry result = InvestigateTipSelector.select(this.store, this.observerUUID, this.neutralReputation, NOW_TICK);

        // Assert
        assertNull(result);
    }

    @Test
    void select_returnsNullWhenAllTipsResolved() {
        // Arrange: only resolved hearsay
        UUID sourceId = UUID.randomUUID();
        this.store.admit(hearsayEntry(UUID.randomUUID(), sourceId, 2.0f, KnowledgeResolution.CONFIRMED));
        this.store.admit(hearsayEntry(UUID.randomUUID(), sourceId, 2.0f, KnowledgeResolution.REFUTED));

        // Act
        KnowledgeEntry result = InvestigateTipSelector.select(this.store, this.observerUUID, this.neutralReputation, NOW_TICK);

        // Assert
        assertNull(result);
    }

    // -------------------------------------------------------------------------
    // select() — picks highest-weight tip
    // -------------------------------------------------------------------------

    @Test
    void select_returnsHighestWeightPendingTip() {
        // Arrange: two unresolved hearsay tips with different weights
        UUID sourceId = UUID.randomUUID();
        UUID lowWeightId = UUID.randomUUID();
        UUID highWeightId = UUID.randomUUID();

        KnowledgeEntry lowWeightTip = hearsayEntry(lowWeightId, sourceId, 1.0f, null);
        KnowledgeEntry highWeightTip = hearsayEntry(highWeightId, sourceId, 3.0f, null);

        this.store.admit(lowWeightTip);
        this.store.admit(highWeightTip);

        // Act
        KnowledgeEntry result = InvestigateTipSelector.select(this.store, this.observerUUID, this.neutralReputation, NOW_TICK);

        // Assert
        assertNotNull(result);
        assertEquals(highWeightId, result.getOriginObservationId());
    }

    @Test
    void select_skipsResolvedTipsWhenPickingBest() {
        // Arrange: one resolved high-weight and one unresolved low-weight
        UUID sourceId = UUID.randomUUID();
        UUID resolvedId = UUID.randomUUID();
        UUID pendingId = UUID.randomUUID();

        this.store.admit(hearsayEntry(resolvedId, sourceId, 5.0f, KnowledgeResolution.CONFIRMED));
        this.store.admit(hearsayEntry(pendingId, sourceId, 1.0f, null));

        // Act
        KnowledgeEntry result = InvestigateTipSelector.select(this.store, this.observerUUID, this.neutralReputation, NOW_TICK);

        // Assert: resolved entry must be skipped even though it has higher weight
        assertNotNull(result);
        assertEquals(pendingId, result.getOriginObservationId());
    }

    // -------------------------------------------------------------------------
    // select() — credibility discount
    // -------------------------------------------------------------------------

    @Test
    void select_appliesCredibilityMultiplierToRanking() {
        // Arrange: two tips with equal raw weight, but sources with different credibility.
        // The low-credibility source's tip should rank below the high-credibility source's.
        UUID trustedSourceId = UUID.randomUUID();
        UUID suspectSourceId = UUID.randomUUID();
        UUID tipFromTrustedId = UUID.randomUUID();
        UUID tipFromSuspectId = UUID.randomUUID();

        // Both tips have the same weight before credibility adjustment
        this.store.admit(hearsayEntry(tipFromTrustedId, trustedSourceId, 2.0f, null));
        this.store.admit(hearsayEntry(tipFromSuspectId, suspectSourceId, 2.0f, null));

        // Reputation: trusted source gets 1.5x multiplier, suspect gets 0.3x
        ReputationQuery biasedReputation = (observer, source) -> {
            if (source.equals(trustedSourceId)) {
                return 1.5f;
            }
            if (source.equals(suspectSourceId)) {
                return 0.3f;
            }
            return 1.0f;
        };

        // Act
        KnowledgeEntry result = InvestigateTipSelector.select(this.store, this.observerUUID, biasedReputation, NOW_TICK);

        // Assert: tip from trusted source should be selected (2.0 × 1.5 = 3.0 > 2.0 × 0.3 = 0.6)
        assertNotNull(result);
        assertEquals(tipFromTrustedId, result.getOriginObservationId(),
                "Selector should prefer the tip from the more credible source");
    }

    @Test
    void select_nearZeroCredibility_stillReturnsTip_butRanksLow() {
        // Arrange: one barely-credible tip and one neutral-credibility tip
        UUID suspectSource = UUID.randomUUID();
        UUID neutralSource = UUID.randomUUID();
        UUID suspectTipId = UUID.randomUUID();
        UUID neutralTipId = UUID.randomUUID();

        // Suspect tip has much higher raw weight but near-zero credibility
        this.store.admit(hearsayEntry(suspectTipId, suspectSource, 10.0f, null));
        this.store.admit(hearsayEntry(neutralTipId, neutralSource, 1.0f, null));

        // Near-zero credibility for suspect source: 10.0 × 0.05 = 0.5 < 1.0 × 1.0
        ReputationQuery query = (observer, source) -> source.equals(suspectSource) ? 0.05f : 1.0f;

        // Act
        KnowledgeEntry result = InvestigateTipSelector.select(this.store, this.observerUUID, query, NOW_TICK);

        // Assert: neutral tip wins despite lower raw weight
        assertNotNull(result);
        assertEquals(neutralTipId, result.getOriginObservationId(),
                "Neutral-credibility tip should outrank near-zero-credibility tip even with lower raw weight");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static KnowledgeEntry directEntry(UUID originId, float weight) {
        return KnowledgeEntry.fromDirectObservation(
                originId, "direct observation", ObservationType.RESOURCE,
                100L, 100L, null, Map.of(), weight);
    }

    /**
     * Builds a hearsay entry directly at hop=1 using fromHearsay so the resolution
     * can be set to a specific value via the builder post-construction.
     */
    private static KnowledgeEntry hearsayEntry(UUID originId, UUID sourceId, float weight,
                                               KnowledgeResolution resolution) {
        KnowledgeEntry base = directEntry(originId, weight);
        KnowledgeEntry hearsay = KnowledgeEntry.fromHearsay(base, sourceId, 200L, weight);

        // fromHearsay always sets resolution = UNRESOLVED; manually resolve when needed
        if (resolution != null && resolution != KnowledgeResolution.UNRESOLVED) {
            hearsay.resolve(resolution);
        }
        return hearsay;
    }

}
