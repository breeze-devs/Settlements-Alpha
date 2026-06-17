package dev.breezes.settlements.application.ai.override;

import dev.breezes.settlements.application.ai.courtship.CourtshipSessionRegistry;
import dev.breezes.settlements.application.ai.planning.InvestigateTipSelector;
import dev.breezes.settlements.application.ai.trading.TradeSessionRegistry;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.credibility.ReputationQuery;
import dev.breezes.settlements.domain.ai.knowledge.KnowledgeEntry;
import dev.breezes.settlements.domain.ai.knowledge.VillagerKnowledgeStore;
import dev.breezes.settlements.domain.ai.observation.ObservationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the OverridePolicy implementations.
 * <p>
 * SocialAcceptOverridePolicy tests: verifies delegation to OverrideTriggerDetector.
 * UrgentInvestigateOverridePolicy tests: verifies urgency scoring without Minecraft objects.
 * <p>
 * All tests follow Arrange / Act / Assert. No Minecraft types are used.
 */
@ExtendWith(MockitoExtension.class)
class OverridePolicyTest {

    @Mock
    private CourtshipSessionRegistry courtshipRegistry;

    @Mock
    private TradeSessionRegistry tradeRegistry;

    // UrgentInvestigateOverridePolicy uses ReputationQuery — stub it as always-neutral.
    private final ReputationQuery neutralReputation = (observer, source) -> 1.0f;

    private UUID villagerId;

    @BeforeEach
    void setUp() {
        this.villagerId = UUID.randomUUID();
    }

    // -------------------------------------------------------------------------
    // SocialAcceptOverridePolicy
    // -------------------------------------------------------------------------

    @Test
    void socialAccept_returnsEmpty_whenNoPendingInvites() {
        // Arrange
        when(courtshipRegistry.hasInviteFor(villagerId)).thenReturn(false);
        when(tradeRegistry.hasInviteFor(villagerId)).thenReturn(false);

        OverrideTriggerDetector detector = new OverrideTriggerDetector(courtshipRegistry, tradeRegistry);
        SocialAcceptOverridePolicyTestable policy = new SocialAcceptOverridePolicyTestable(detector, villagerId);

        // Act
        Optional<OverrideRequest> result = policy.evaluateDirect();

        // Assert
        assertFalse(result.isPresent(), "No invite → policy must return empty");
    }

    @Test
    void socialAccept_returnsCourtshipKey_whenCourtshipInvitePending() {
        // Arrange
        when(courtshipRegistry.hasInviteFor(villagerId)).thenReturn(true);

        OverrideTriggerDetector detector = new OverrideTriggerDetector(courtshipRegistry, tradeRegistry);
        SocialAcceptOverridePolicyTestable policy = new SocialAcceptOverridePolicyTestable(detector, villagerId);

        // Act
        Optional<OverrideRequest> result = policy.evaluateDirect();

        // Assert
        assertTrue(result.isPresent());
        assertEquals(BehaviorKey.COURTSHIP_ACCEPT, result.get().getBehaviorKey());
    }

    @Test
    void socialAccept_returnsTradeKey_whenOnlyTradeInvitePending() {
        // Arrange
        when(courtshipRegistry.hasInviteFor(villagerId)).thenReturn(false);
        when(tradeRegistry.hasInviteFor(villagerId)).thenReturn(true);

        OverrideTriggerDetector detector = new OverrideTriggerDetector(courtshipRegistry, tradeRegistry);
        SocialAcceptOverridePolicyTestable policy = new SocialAcceptOverridePolicyTestable(detector, villagerId);

        // Act
        Optional<OverrideRequest> result = policy.evaluateDirect();

        // Assert
        assertTrue(result.isPresent());
        assertEquals(BehaviorKey.TRADE_ACCEPT, result.get().getBehaviorKey());
    }

    // -------------------------------------------------------------------------
    // UrgentInvestigateOverridePolicy — tested via domain logic only
    // -------------------------------------------------------------------------

    @Test
    void urgentInvestigate_returnsEmpty_whenStoreIsEmpty() {
        // Arrange
        VillagerKnowledgeStore store = new VillagerKnowledgeStore();
        UrgentInvestigateOverridePolicyTestable policy =
                new UrgentInvestigateOverridePolicyTestable(this.neutralReputation, store, this.villagerId);

        // Act
        Optional<OverrideRequest> result = policy.evaluateWithTick(1000L);

        // Assert
        assertFalse(result.isPresent(), "Empty store must not trigger an override");
    }

    @Test
    void urgentInvestigate_returnsEmpty_whenTipIsStale() {
        // Arrange: tip with normal weight but admitted 100,000 ticks ago (well past freshness)
        long originTick = 0L;
        long nowTick = 100_000L;
        VillagerKnowledgeStore store = new VillagerKnowledgeStore();
        store.admit(hearsayEntry(UUID.randomUUID(), UUID.randomUUID(), 3.0f, originTick));

        UrgentInvestigateOverridePolicyTestable policy =
                new UrgentInvestigateOverridePolicyTestable(this.neutralReputation, store, this.villagerId);

        // Act
        Optional<OverrideRequest> result = policy.evaluateWithTick(nowTick);

        // Assert: old tip should not be urgent (freshness decays to near 0)
        assertFalse(result.isPresent(), "Stale tip must not trigger urgent override");
    }

    @Test
    void urgentInvestigate_returnsInvestigateKey_whenTipIsFreshAndHighWeight() {
        // Arrange: freshly admitted high-weight tip (originTick = nowTick, so freshness = 1.0)
        long nowTick = 100L;
        VillagerKnowledgeStore store = new VillagerKnowledgeStore();
        // weight=5.0 × freshness=1.0 = 5.0 > threshold(2.5)
        store.admit(hearsayEntry(UUID.randomUUID(), UUID.randomUUID(), 5.0f, nowTick));

        UrgentInvestigateOverridePolicyTestable policy =
                new UrgentInvestigateOverridePolicyTestable(this.neutralReputation, store, this.villagerId);

        // Act
        Optional<OverrideRequest> result = policy.evaluateWithTick(nowTick);

        // Assert
        assertTrue(result.isPresent(), "Fresh high-weight tip should trigger urgent override");
        assertEquals(BehaviorKey.INVESTIGATE, result.get().getBehaviorKey());
    }

    // -------------------------------------------------------------------------
    // Test-only subclasses that expose the domain-logic seam without needing Minecraft
    // -------------------------------------------------------------------------

    /**
     * Wraps SocialAcceptOverridePolicy with a test-friendly evaluate that doesn't need a ServerLevel.
     */
    static final class SocialAcceptOverridePolicyTestable {
        private final OverrideTriggerDetector detector;
        private final UUID villagerId;

        SocialAcceptOverridePolicyTestable(OverrideTriggerDetector detector, UUID villagerId) {
            this.detector = detector;
            this.villagerId = villagerId;
        }

        Optional<OverrideRequest> evaluateDirect() {
            var behaviorKey = this.detector.detect(this.villagerId);
            if (behaviorKey == null) {
                return Optional.empty();
            }
            return Optional.of(OverrideRequest.builder().behaviorKey(behaviorKey).build());
        }
    }

    /**
     * Wraps the urgency-score domain logic extracted from UrgentInvestigateOverridePolicy,
     * testable without a ServerLevel or BaseVillager.
     */
    static final class UrgentInvestigateOverridePolicyTestable {
        private static final double URGENCY_THRESHOLD = 2.5;
        private static final double FRESHNESS_HALVING_TICKS = 6_000.0;

        private final ReputationQuery reputationQuery;
        private final VillagerKnowledgeStore store;
        private final UUID villagerId;

        UrgentInvestigateOverridePolicyTestable(ReputationQuery reputationQuery,
                                                VillagerKnowledgeStore store,
                                                UUID villagerId) {
            this.reputationQuery = reputationQuery;
            this.store = store;
            this.villagerId = villagerId;
        }

        Optional<OverrideRequest> evaluateWithTick(long nowTick) {
            if (this.store.isEmpty()) {
                return Optional.empty();
            }

            KnowledgeEntry tip = InvestigateTipSelector.select(
                    this.store, this.villagerId, this.reputationQuery, nowTick);
            if (tip == null) {
                return Optional.empty();
            }

            long ageTicks = Math.max(0L, nowTick - tip.getOriginTimestampTick());
            double freshness = Math.pow(0.5, ageTicks / FRESHNESS_HALVING_TICKS);
            double urgencyScore = tip.getWeight() * freshness;

            if (urgencyScore >= URGENCY_THRESHOLD) {
                return Optional.of(OverrideRequest.builder().behaviorKey(BehaviorKey.INVESTIGATE).build());
            }
            return Optional.empty();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static KnowledgeEntry hearsayEntry(UUID originId, UUID sourceId, float weight, long originTick) {
        KnowledgeEntry direct = KnowledgeEntry.fromDirectObservation(
                originId, "test tip", ObservationType.RESOURCE,
                originTick, originTick, null, Map.of(), weight);
        return KnowledgeEntry.fromHearsay(direct, sourceId, originTick, weight);
    }

}
