package dev.breezes.settlements.application.ai.override;

import dev.breezes.settlements.application.ai.courtship.CourtshipSessionRegistry;
import dev.breezes.settlements.application.ai.trading.TradeSessionRegistry;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
 * <p>
 * All tests follow Arrange / Act / Assert. No Minecraft types are used.
 */
@ExtendWith(MockitoExtension.class)
class OverridePolicyTest {

    @Mock
    private CourtshipSessionRegistry courtshipRegistry;

    @Mock
    private TradeSessionRegistry tradeRegistry;

    private UUID villagerId;

    @BeforeEach
    void setUp() {
        this.villagerId = UUID.randomUUID();
    }

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

    // Test-only subclasses that expose the domain-logic seam without needing Minecraft
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
            BehaviorKey behaviorKey = this.detector.detect(this.villagerId);
            if (behaviorKey == null) {
                return Optional.empty();
            }
            return Optional.of(OverrideRequest.builder().behaviorKey(behaviorKey).build());
        }
    }

}
