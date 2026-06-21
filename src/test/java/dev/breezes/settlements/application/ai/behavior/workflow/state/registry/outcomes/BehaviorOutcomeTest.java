package dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes;

import dev.breezes.settlements.domain.ai.worldevent.EventOutcome;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.RESOURCE_HARVESTED, "pumpkins");

        // Assert
        assertFalse(outcome.isSuccess());
        assertEquals(WorldEventType.RESOURCE_HARVESTED, outcome.getDeedType());
        assertEquals("pumpkins", outcome.getUnitNoun());
    }

    @Test
    void recordYield_accumulatesMagnitudeAndDerivesDetail() {
        // Arrange
        BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.RESOURCE_HARVESTED, "pumpkins");

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
    void forDeedWithQualifier_appendsQualifierAfterCountedNoun() {
        // Arrange
        BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.SHEEP_DYED, "sheep", "blue");

        // Act
        outcome.recordYield(3);

        // Assert
        assertEquals("3 sheep blue", outcome.resolveDetail());
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
    void recordTargetedDeed_setsPartnerIdAndMarksSuccess() {
        // Arrange
        BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.TARGET_EGGED, null);
        UUID victimId = UUID.randomUUID();

        // Act
        outcome.recordTargetedDeed(victimId);

        // Assert
        assertTrue(outcome.isSuccess());
        assertEquals(EventOutcome.SUCCESS, outcome.getEventOutcome());
        assertEquals(victimId, outcome.getPartnerId());
        assertNotNull(outcome.getPartnerId());
    }

    @Test
    void recordTargetedDeed_doesNotSetDetail() {
        // Arrange — targeted deed has no unit noun, so detail should remain unresolvable
        BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.POTION_THROWN, null);
        UUID victimId = UUID.randomUUID();

        // Act
        outcome.recordTargetedDeed(victimId);

        // Assert — no unit noun set, so resolveDetail returns null (target flows via partnerId)
        assertNull(outcome.resolveDetail());
    }

    @Test
    void recordDeedDetail_setsExplicitDetailAndMarksSuccess() {
        // Arrange — a non-counted deed whose phrasing is a plain noun (e.g. "tamed a cat")
        BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.ANIMAL_TAMED, null);

        // Act
        outcome.recordDeedDetail("a cat");

        // Assert
        assertTrue(outcome.isSuccess());
        assertEquals(EventOutcome.SUCCESS, outcome.getEventOutcome());
        assertEquals("a cat", outcome.resolveDetail());
    }

    @Test
    void reset_clearsRunResultButKeepsDeclaredDeed() {
        // Arrange
        BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.RESOURCE_HARVESTED, "pumpkins");
        outcome.recordYield(3);

        // Act
        outcome.reset();

        // Assert
        assertFalse(outcome.isSuccess());
        assertEquals(0, outcome.getMagnitude());
        assertEquals(WorldEventType.RESOURCE_HARVESTED, outcome.getDeedType());
        assertEquals("pumpkins", outcome.getUnitNoun());
        assertNull(outcome.getEventOutcome());
        assertEquals("0 pumpkins", outcome.resolveDetail());
    }

}
