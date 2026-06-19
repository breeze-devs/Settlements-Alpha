package dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes;

import dev.breezes.settlements.domain.ai.worldevent.EventOutcome;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BehaviorOutcomeTest {

    @Test
    void blank_createsUnsuccessfulOutcomeWithoutDeclaredDeed() {
        // Arrange, Act
        BehaviorOutcome outcome = BehaviorOutcome.blank();

        // Assert
        assertFalse(outcome.isSuccess());
        assertNull(outcome.getDeedType());
        assertNull(outcome.resolveDetail());
    }

    @Test
    void forDeed_recordsDeclaredDeedWithoutMarkingSuccess() {
        // Arrange, Act
        BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.CROP_HARVESTED, "pumpkins");

        // Assert
        assertFalse(outcome.isSuccess());
        assertEquals(WorldEventType.CROP_HARVESTED, outcome.getDeedType());
        assertEquals("pumpkins", outcome.getUnitNoun());
    }

    @Test
    void recordYield_accumulatesMagnitudeAndDerivesDetail() {
        // Arrange
        BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.CROP_HARVESTED, "pumpkins");

        // Act
        outcome.recordYield(2);
        outcome.recordYield(3);

        // Assert
        assertTrue(outcome.isSuccess());
        assertEquals(EventOutcome.SUCCESS, outcome.getEventOutcome());
        assertEquals(5, outcome.getMagnitude());
        assertEquals("5 pumpkins", outcome.resolveDetail());
    }

    @Test
    void recordSocialOutcome_capturesPartnerRegistryOutcomeAndDetail() {
        // Arrange
        BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.TRADE_COMPLETED, null);
        UUID partnerId = UUID.randomUUID();
        UUID registryId = UUID.randomUUID();

        // Act
        outcome.recordSocialOutcome(partnerId, registryId, EventOutcome.SUCCESS, "4 bread for 1 emerald", null);

        // Assert
        assertTrue(outcome.isSuccess());
        assertEquals(partnerId, outcome.getPartnerId());
        assertEquals(registryId, outcome.getRegistryId());
        assertEquals(EventOutcome.SUCCESS, outcome.getEventOutcome());
        assertEquals("4 bread for 1 emerald", outcome.resolveDetail());
    }

    @Test
    void reset_clearsRunResultButKeepsDeclaredDeed() {
        // Arrange
        BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.CROP_HARVESTED, "pumpkins");
        outcome.recordYield(3);

        // Act
        outcome.reset();

        // Assert
        assertFalse(outcome.isSuccess());
        assertEquals(0, outcome.getMagnitude());
        assertEquals(WorldEventType.CROP_HARVESTED, outcome.getDeedType());
        assertEquals("pumpkins", outcome.getUnitNoun());
        assertNull(outcome.getEventOutcome());
        assertEquals("0 pumpkins", outcome.resolveDetail());
    }

}
