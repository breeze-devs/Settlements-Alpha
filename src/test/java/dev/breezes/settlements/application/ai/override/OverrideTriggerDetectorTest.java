package dev.breezes.settlements.application.ai.override;

import dev.breezes.settlements.application.ai.courtship.CourtshipSessionRegistry;
import dev.breezes.settlements.application.ai.trading.TradeSessionRegistry;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the registry-poll override trigger detector. The registries are mocked because
 * they are the authoritative source of pending invites; the detector itself is pure-domain.
 */
@ExtendWith(MockitoExtension.class)
class OverrideTriggerDetectorTest {

    @Mock
    private CourtshipSessionRegistry courtshipRegistry;

    @Mock
    private TradeSessionRegistry tradeRegistry;

    private OverrideTriggerDetector detector;
    private UUID villagerId;

    @BeforeEach
    void setUp() {
        this.detector = new OverrideTriggerDetector(courtshipRegistry, tradeRegistry);
        this.villagerId = UUID.randomUUID();
    }

    @Test
    void detect_returnsNull_whenNoInvitePending() {
        // Arrange
        when(courtshipRegistry.hasInviteFor(villagerId)).thenReturn(false);
        when(tradeRegistry.hasInviteFor(villagerId)).thenReturn(false);

        // Act
        BehaviorKey result = detector.detect(villagerId);

        // Assert
        assertNull(result);
    }

    @Test
    void detect_returnsCourtshipAccept_whenCourtshipInvitePending() {
        // Arrange — courtship pending; trade is never consulted because courtship short-circuits
        when(courtshipRegistry.hasInviteFor(villagerId)).thenReturn(true);

        // Act
        BehaviorKey result = detector.detect(villagerId);

        // Assert
        assertEquals(BehaviorKey.COURTSHIP_ACCEPT, result);
    }

    @Test
    void detect_returnsTradeAccept_whenOnlyTradeInvitePending() {
        // Arrange
        when(courtshipRegistry.hasInviteFor(villagerId)).thenReturn(false);
        when(tradeRegistry.hasInviteFor(villagerId)).thenReturn(true);

        // Act
        BehaviorKey result = detector.detect(villagerId);

        // Assert
        assertEquals(BehaviorKey.TRADE_ACCEPT, result);
    }

    @Test
    void detect_returnsCourtshipAccept_whenBothPending_becauseCourtshipHasPriority() {
        // Arrange — both pending; trade is never consulted because courtship wins first
        when(courtshipRegistry.hasInviteFor(villagerId)).thenReturn(true);

        // Act
        BehaviorKey result = detector.detect(villagerId);

        // Assert
        assertEquals(BehaviorKey.COURTSHIP_ACCEPT, result);
    }

}
