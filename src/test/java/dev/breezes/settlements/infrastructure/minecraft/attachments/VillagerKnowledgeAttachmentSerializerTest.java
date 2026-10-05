package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.domain.ai.knowledge.KnowledgeEntry;
import dev.breezes.settlements.domain.ai.knowledge.VillagerKnowledgeStore;
import dev.breezes.settlements.domain.ai.memory.PackedPos;
import dev.breezes.settlements.domain.ai.observation.Observation;
import dev.breezes.settlements.domain.ai.observation.ObservationType;
import dev.breezes.settlements.domain.ai.perception.ObservationFactory;
import dev.breezes.settlements.domain.ai.worldevent.WorldEvent;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The entry's semantic type is not persisted (see {@link KnowledgeEntryState}), so reading must derive it from the
 * metadata to equal the pre-save value exactly. Reading a whole store needs the configured capacity from the Dagger
 * graph, so these tests drive the derivation helper and the write mapping directly.
 */
class VillagerKnowledgeAttachmentSerializerTest {

    @Test
    void resolveObservationType_equalsPreSaveValue() {
        // Arrange — build an entry the same way PerceptionPipeline does, then flatten it the way a save does
        // (semantic type dropped)
        WorldEvent event = WorldEvent.builder()
                .sequence(1L)
                .gameTick(100L)
                .type(WorldEventType.LEATHER_WASHED)
                .actorId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .metadata("wash_leather")
                .posX(10.0).posY(64.0).posZ(-5.0)
                .chunkX(0).chunkZ(0)
                .build();
        Observation observation = ObservationFactory.fromEvent(event, 100L);
        Map<String, String> metadata = ObservationFactory.metadataFor(observation);
        KnowledgeEntry preSave = KnowledgeEntry.fromDirectObservation(
                observation.id(), observation.type(),
                100L, 100L, null, metadata, 2.4f, PackedPos.asLong(10, 64, -5));
        VillagerKnowledgeStore store = new VillagerKnowledgeStore(10);
        store.admit(preSave);

        KnowledgeEntryState state = new VillagerKnowledgeAttachmentSerializer().toState(store).getFirst();

        // Act
        ObservationType reconstructedType = VillagerKnowledgeAttachmentSerializer.resolveObservationType(state.metadata());

        // Assert
        assertEquals(preSave.getType(), reconstructedType);
    }

    @Test
    void toState_keepsAdmissionOrder() {
        // Arrange — reading admits entries in saved order, and the store evicts its oldest admission first, so a
        // reordered save would make a reload evict the wrong entries
        UUID first = UUID.fromString("00000000-0000-0000-0000-00000000000c");
        UUID second = UUID.fromString("00000000-0000-0000-0000-00000000000a");
        UUID third = UUID.fromString("00000000-0000-0000-0000-00000000000b");
        VillagerKnowledgeStore store = new VillagerKnowledgeStore(10);
        store.admit(entry(first));
        store.admit(entry(second));
        store.admit(entry(third));

        // Act
        List<KnowledgeEntryState> saved = new VillagerKnowledgeAttachmentSerializer().toState(store);

        // Assert
        List<UUID> savedOrder = saved.stream().map(KnowledgeEntryState::originObservationId).toList();
        assertEquals(List.of(first, second, third), savedOrder);
    }

    @Test
    void resolveObservationType_unrecognizedEventType_returnsNull() {
        // Arrange — mirrors an entry whose event type was retired from WorldEventType
        Map<String, String> metadata = Map.of("event_type", "REMOVED_EVENT_TYPE");

        // Act
        ObservationType type = VillagerKnowledgeAttachmentSerializer.resolveObservationType(metadata);

        // Assert — reading must drop this entry rather than admit it with a fallback
        assertNull(type);
    }

    @Test
    void resolveObservationType_missingEventType_returnsNull() {
        // Arrange
        Map<String, String> metadata = Map.of();

        // Act
        ObservationType type = VillagerKnowledgeAttachmentSerializer.resolveObservationType(metadata);

        // Assert
        assertNull(type);
    }

    @Test
    void resolveObservationType_knownEventType_derivesMatchingObservationType() {
        // Arrange
        Map<String, String> metadata = Map.of("event_type", WorldEventType.TRADE_COMPLETED.name());

        // Act
        ObservationType type = VillagerKnowledgeAttachmentSerializer.resolveObservationType(metadata);

        // Assert
        assertEquals(WorldEventType.TRADE_COMPLETED.getObservationType(), type);
    }

    private static KnowledgeEntry entry(UUID originId) {
        return KnowledgeEntry.fromDirectObservation(
                originId,
                ObservationType.RESOURCE,
                100L,
                100L,
                null,
                Map.of("event_type", WorldEventType.RESOURCE_HARVESTED.name()),
                1.0f,
                null);
    }

}
