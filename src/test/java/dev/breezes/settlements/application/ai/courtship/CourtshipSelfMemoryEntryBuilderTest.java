package dev.breezes.settlements.application.ai.courtship;

import dev.breezes.settlements.domain.ai.knowledge.KnowledgeEntry;
import dev.breezes.settlements.domain.ai.observation.ObservationMetadataKeys;
import dev.breezes.settlements.domain.ai.observation.ObservationType;
import dev.breezes.settlements.domain.ai.worldevent.EventOutcome;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link CourtshipSelfMemoryEntryBuilder}.
 * <p>
 * Verifies that the metadata map and entry structure are correct for a private courtship failure
 * recorded off-bus. No Minecraft types are used.
 */
class CourtshipSelfMemoryEntryBuilderTest {

    // -------------------------------------------------------------------------
    // Metadata keys
    // -------------------------------------------------------------------------

    @Test
    void buildMetadata_containsEventTypeKey() {
        // Arrange
        UUID actorId = UUID.randomUUID();
        String reason = "no one answered";

        // Act
        Map<String, String> metadata = CourtshipSelfMemoryEntryBuilder.buildMetadata(actorId, reason);

        // Assert
        assertEquals(WorldEventType.COURTSHIP_COMPLETED.name(), metadata.get(ObservationMetadataKeys.EVENT_TYPE));
    }

    @Test
    void buildMetadata_containsActorIdKey() {
        // Arrange
        UUID actorId = UUID.randomUUID();

        // Act
        Map<String, String> metadata = CourtshipSelfMemoryEntryBuilder.buildMetadata(actorId, "no one answered");

        // Assert
        assertEquals(actorId.toString(), metadata.get(ObservationMetadataKeys.ACTOR_ID));
    }

    @Test
    void buildMetadata_containsFailureOutcome() {
        // Arrange
        UUID actorId = UUID.randomUUID();

        // Act
        Map<String, String> metadata = CourtshipSelfMemoryEntryBuilder.buildMetadata(actorId, "no one answered");

        // Assert
        assertEquals(EventOutcome.FAILURE.name(), metadata.get(ObservationMetadataKeys.OUTCOME));
    }

    @Test
    void buildMetadata_containsReason() {
        // Arrange
        UUID actorId = UUID.randomUUID();
        String reason = "no one answered";

        // Act
        Map<String, String> metadata = CourtshipSelfMemoryEntryBuilder.buildMetadata(actorId, reason);

        // Assert
        assertEquals(reason, metadata.get(ObservationMetadataKeys.REASON));
    }

    // -------------------------------------------------------------------------
    // Entry shape
    // -------------------------------------------------------------------------

    @Test
    void build_entryHasHopZero() {
        // Arrange
        UUID actorId = UUID.randomUUID();
        UUID originId = UUID.randomUUID();

        // Act
        KnowledgeEntry entry = CourtshipSelfMemoryEntryBuilder.build(actorId, null, 1000L, "no one answered", originId);

        // Assert – hop 0 marks this as a first-hand observation, never hearsay
        assertEquals(0, entry.getHop());
    }

    @Test
    void build_entryHasNullSource() {
        // Arrange
        UUID actorId = UUID.randomUUID();
        UUID originId = UUID.randomUUID();

        // Act
        KnowledgeEntry entry = CourtshipSelfMemoryEntryBuilder.build(actorId, null, 1000L, "no one answered", originId);

        // Assert – null source confirms this was not shared by a gossip partner
        assertNull(entry.getSource());
    }

    @Test
    void build_entryUsesSuppliedOriginObservationId() {
        // Arrange
        UUID actorId = UUID.randomUUID();
        UUID originId = UUID.randomUUID();

        // Act
        KnowledgeEntry entry = CourtshipSelfMemoryEntryBuilder.build(actorId, null, 1000L, "no one answered", originId);

        // Assert
        assertEquals(originId, entry.getOriginObservationId());
    }

    @Test
    void build_entryTypeIsSocial() {
        // Arrange
        UUID actorId = UUID.randomUUID();
        UUID originId = UUID.randomUUID();

        // Act
        KnowledgeEntry entry = CourtshipSelfMemoryEntryBuilder.build(actorId, null, 1000L, "no one answered", originId);

        // Assert – courtship is a social act regardless of outcome
        assertEquals(ObservationType.SOCIAL, entry.getType());
    }

    @Test
    void build_entryWeightMatchesBusBaseImportance() {
        // Arrange
        UUID actorId = UUID.randomUUID();
        UUID originId = UUID.randomUUID();

        // Act
        KnowledgeEntry entry = CourtshipSelfMemoryEntryBuilder.build(actorId, null, 1000L, "no one answered", originId);

        // Assert – weight must equal the base importance ObservationFactory assigns to COURTSHIP_COMPLETED
        // on the bus (2.5F), so the villager weights its own private memory consistently with witnessed events
        assertEquals(CourtshipSelfMemoryEntryBuilder.SELF_FAILURE_WEIGHT, entry.getWeight(), 0.001f);
    }

    @Test
    void build_entryMetadataContainsRequiredKeys() {
        // Arrange
        UUID actorId = UUID.randomUUID();
        UUID originId = UUID.randomUUID();
        String reason = "no one answered";

        // Act
        KnowledgeEntry entry = CourtshipSelfMemoryEntryBuilder.build(actorId, null, 1000L, reason, originId);

        // Assert – all four metadata keys the projector reads must be present
        Map<String, String> metadata = entry.getMetadata();
        assertNotNull(metadata.get(ObservationMetadataKeys.EVENT_TYPE));
        assertNotNull(metadata.get(ObservationMetadataKeys.ACTOR_ID));
        assertNotNull(metadata.get(ObservationMetadataKeys.OUTCOME));
        assertNotNull(metadata.get(ObservationMetadataKeys.REASON));
    }

    @Test
    void build_entryRelatedEntityIsPartnerWhenProvided() {
        // Arrange
        UUID actorId = UUID.randomUUID();
        UUID partnerId = UUID.randomUUID();
        UUID originId = UUID.randomUUID();

        // Act
        KnowledgeEntry entry = CourtshipSelfMemoryEntryBuilder.build(actorId, partnerId, 1000L, "no one answered", originId);

        // Assert
        assertEquals(partnerId, entry.getRelatedEntity());
    }

    @Test
    void build_entryRelatedEntityIsNullWhenPartnerAbsent() {
        // Arrange
        UUID actorId = UUID.randomUUID();
        UUID originId = UUID.randomUUID();

        // Act
        KnowledgeEntry entry = CourtshipSelfMemoryEntryBuilder.build(actorId, null, 1000L, "no one answered", originId);

        // Assert
        assertNull(entry.getRelatedEntity());
    }

}
