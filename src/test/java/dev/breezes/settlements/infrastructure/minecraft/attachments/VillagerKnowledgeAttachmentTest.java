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

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies the load-time reconstruction logic in {@link VillagerKnowledgeAttachment}: type and
 * weight are dropped from persistence (see {@link KnowledgeEntryState}) and must be recomputed on
 * load to equal the pre-save values exactly.
 * <p>
 * {@link VillagerKnowledgeAttachment#saveFrom}/{@code loadInto} themselves require a
 * {@link dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager}, which
 * is unmockable Minecraft state — so this test exercises the same reconstruction helpers
 * ({@link VillagerKnowledgeAttachment#resolveObservationType}, {@link KnowledgeEntry#recomputeWeight})
 * that {@code loadInto} calls internally, driven from a real {@link KnowledgeEntryState} built the
 * same way {@code saveFrom} would build one.
 */
class VillagerKnowledgeAttachmentTest {

    @Test
    void reconstruction_typeAndWeight_equalPreSaveValues() {
        // Arrange — build a first-hand entry the same way PerceptionPipeline does, then simulate
        // the "state" it would be flattened into for persistence (type/weight dropped).
        UUID actorId = UUID.randomUUID();
        WorldEvent event = WorldEvent.builder()
                .sequence(1L)
                .gameTick(100L)
                .type(WorldEventType.LEATHER_WASHED)
                .actorId(actorId)
                .metadata("wash_leather")
                .posX(10.0).posY(64.0).posZ(-5.0)
                .chunkX(0).chunkZ(0)
                .build();
        Observation observation = ObservationFactory.fromEvent(event, 100L);
        Map<String, String> metadata = ObservationFactory.metadataFor(observation);
        long packedPos = PackedPos.asLong(10, 64, -5);

        KnowledgeEntry preSave = KnowledgeEntry.fromDirectObservation(
                observation.id(), observation.type(),
                100L, 100L, null, metadata, 2.4f, packedPos);
        preSave.corroborate(VillagerKnowledgeStore.CORROBORATION_BUMP);

        KnowledgeEntryState state = KnowledgeEntryState.builder()
                .originObservationId(preSave.getOriginObservationId())
                .originTimestampTick(preSave.getOriginTimestampTick())
                .admittedAtTick(preSave.getAdmittedAtTick())
                .relatedEntity(preSave.getRelatedEntity())
                .metadata(preSave.getMetadata())
                .packedPos(preSave.getPackedPos())
                .source(preSave.getSource())
                .hop(preSave.getHop())
                .originalWeight(preSave.getOriginalWeight())
                .corroborationCount(preSave.getCorroborationCount())
                .build();

        // Act — reconstruct exactly as VillagerKnowledgeAttachment.loadInto does
        ObservationType reconstructedType = VillagerKnowledgeAttachment.resolveObservationType(state.metadata());
        float reconstructedWeight = KnowledgeEntry.recomputeWeight(
                state.originalWeight(), state.corroborationCount(), VillagerKnowledgeStore.CORROBORATION_BUMP);

        // Assert — reconstructed values equal the pre-save values
        assertEquals(preSave.getType(), reconstructedType);
        assertEquals(preSave.getWeight(), reconstructedWeight, 0.0001f);
    }

    @Test
    void reconstruction_packedPos_roundTripsThroughState() {
        // Arrange
        long packedPos = PackedPos.asLong(-100, 12, 200);
        KnowledgeEntryState state = KnowledgeEntryState.builder()
                .originObservationId(UUID.randomUUID())
                .originTimestampTick(0L)
                .admittedAtTick(0L)
                .metadata(Map.of("event_type", WorldEventType.RESOURCE_HARVESTED.name()))
                .packedPos(packedPos)
                .hop(0)
                .originalWeight(1.0f)
                .corroborationCount(0)
                .build();

        // Act, Assert
        assertEquals(packedPos, state.packedPos());
    }

    @Test
    void reconstruction_nullPackedPos_staysNull() {
        // Arrange — the off-bus courtship self-failure entry has no spatial grounding
        KnowledgeEntryState state = KnowledgeEntryState.builder()
                .originObservationId(UUID.randomUUID())
                .originTimestampTick(0L)
                .admittedAtTick(0L)
                .metadata(Map.of("event_type", WorldEventType.COURTSHIP_CHILD_BIRTH.name()))
                .packedPos(null)
                .hop(0)
                .originalWeight(1.0f)
                .corroborationCount(0)
                .build();

        // Act, Assert
        assertNull(state.packedPos());
    }

    @Test
    void resolveObservationType_unrecognizedEventType_returnsNull() {
        // Arrange — mirrors an entry whose event type was retired from WorldEventType
        Map<String, String> metadata = Map.of("event_type", "REMOVED_EVENT_TYPE");

        // Act
        ObservationType type = VillagerKnowledgeAttachment.resolveObservationType(metadata);

        // Assert — caller (loadInto) must drop this entry rather than admit it with a fallback
        assertNull(type);
    }

    @Test
    void resolveObservationType_missingEventType_returnsNull() {
        // Arrange
        Map<String, String> metadata = Map.of();

        // Act
        ObservationType type = VillagerKnowledgeAttachment.resolveObservationType(metadata);

        // Assert
        assertNull(type);
    }

    @Test
    void resolveObservationType_knownEventType_derivesMatchingObservationType() {
        // Arrange
        Map<String, String> metadata = Map.of("event_type", WorldEventType.TRADE_COMPLETED.name());

        // Act
        ObservationType type = VillagerKnowledgeAttachment.resolveObservationType(metadata);

        // Assert
        assertEquals(WorldEventType.TRADE_COMPLETED.getObservationType(), type);
    }

}
