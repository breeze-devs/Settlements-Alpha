package dev.breezes.settlements.domain.ai.perception;

import dev.breezes.settlements.application.ai.memory.MemoryImportanceGate;
import dev.breezes.settlements.domain.ai.observation.Observation;
import dev.breezes.settlements.domain.ai.observation.ObservationMetadataKeys;
import dev.breezes.settlements.domain.ai.observation.ObservationType;
import dev.breezes.settlements.domain.ai.worldevent.EventOutcome;
import dev.breezes.settlements.domain.ai.worldevent.WorldEvent;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservationFactoryTest {

    private static final long CURRENT_TICK = 500L;

    @Test
    void fromEvent_mapsTradeCompletedToSocialObservationType() {
        // Arrange
        WorldEvent event = worldEvent(WorldEventType.TRADE_COMPLETED);

        // Act
        Observation observation = ObservationFactory.fromEvent(event, CURRENT_TICK);

        // Assert
        assertEquals(ObservationType.SOCIAL, observation.type());
    }

    @Test
    void fromEvent_mapsCourtshipChildBirthToSocialObservationType() {
        // Arrange
        WorldEvent event = worldEvent(WorldEventType.COURTSHIP_CHILD_BIRTH);

        // Act
        Observation observation = ObservationFactory.fromEvent(event, CURRENT_TICK);

        // Assert
        assertEquals(ObservationType.SOCIAL, observation.type());
    }

    @Test
    void fromEvent_mapsCropHarvestedToResourceObservationType() {
        // Arrange
        WorldEvent event = worldEvent(WorldEventType.RESOURCE_HARVESTED);

        // Act
        Observation observation = ObservationFactory.fromEvent(event, CURRENT_TICK);

        // Assert
        assertEquals(ObservationType.RESOURCE, observation.type());
    }

    @Test
    void fromEvent_mapsSheepShearedToResourceObservationType() {
        // Arrange
        WorldEvent event = worldEvent(WorldEventType.SHEEP_SHEARED);

        // Act
        Observation observation = ObservationFactory.fromEvent(event, CURRENT_TICK);

        // Assert
        assertEquals(ObservationType.RESOURCE, observation.type());
    }

    @Test
    void fromEvent_mapsSheepDyedToResourceObservationType() {
        // Arrange
        WorldEvent event = worldEvent(WorldEventType.SHEEP_DYED);

        // Act
        Observation observation = ObservationFactory.fromEvent(event, CURRENT_TICK);

        // Assert
        assertEquals(ObservationType.RESOURCE, observation.type());
    }

    @Test
    void fromEvent_mapsFurnaceMisfiredToIncidentObservationType() {
        // Arrange
        WorldEvent event = worldEvent(WorldEventType.FURNACE_MISFIRED);

        // Act
        Observation observation = ObservationFactory.fromEvent(event, CURRENT_TICK);

        // Assert
        assertEquals(ObservationType.INCIDENT, observation.type());
    }

    @Test
    void fromEvent_mapsBehaviorStartedToTaskCompletionObservationType() {
        // Arrange
        WorldEvent event = worldEvent(WorldEventType.BEHAVIOR_STARTED);

        // Act
        Observation observation = ObservationFactory.fromEvent(event, CURRENT_TICK);

        // Assert
        assertEquals(ObservationType.TASK_COMPLETION, observation.type());
    }

    @Test
    void fromEvent_usesProvidedCurrentTickAsTimestamp() {
        // Arrange
        WorldEvent event = worldEvent(WorldEventType.RESOURCE_HARVESTED);

        // Act
        Observation observation = ObservationFactory.fromEvent(event, CURRENT_TICK);

        // Assert
        assertEquals(CURRENT_TICK, observation.timestampTick());
    }

    @Test
    void fromEvent_tradeCompletedBaseImportanceExceedsPromotionThreshold() {
        // Arrange
        WorldEvent event = worldEvent(WorldEventType.TRADE_COMPLETED);

        // Act
        Observation observation = ObservationFactory.fromEvent(event, CURRENT_TICK);

        // Assert — importance alone (before gene/novelty) should exceed the threshold,
        // because trade completions are semantically significant social facts.
        assertTrue(observation.baseImportance() >= MemoryImportanceGate.PROMOTION_THRESHOLD,
                "Trade-completed base importance " + observation.baseImportance() +
                        " should be >= promotion threshold " + MemoryImportanceGate.PROMOTION_THRESHOLD);
    }

    @Test
    void fromEvent_behaviorStartedBaseImportanceBelowPromotionThreshold() {
        // Arrange — routine lifecycle events should not unconditionally promote
        WorldEvent event = worldEvent(WorldEventType.BEHAVIOR_STARTED);

        // Act
        Observation observation = ObservationFactory.fromEvent(event, CURRENT_TICK);

        // Assert
        assertTrue(observation.baseImportance() < MemoryImportanceGate.PROMOTION_THRESHOLD,
                "Behavior-started base importance " + observation.baseImportance() +
                        " should be < promotion threshold " + MemoryImportanceGate.PROMOTION_THRESHOLD);
    }

    @Test
    void metadataFor_containsEventType() {
        // Arrange
        WorldEvent event = worldEvent(WorldEventType.RESOURCE_HARVESTED);
        Observation observation = ObservationFactory.fromEvent(event, CURRENT_TICK);

        // Act
        Map<String, String> metadata = ObservationFactory.metadataFor(observation);

        // Assert
        assertTrue(metadata.containsKey("event_type"),
                "metadata should carry event_type key");
        assertEquals(WorldEventType.RESOURCE_HARVESTED.name(), metadata.get("event_type"));
    }

    @Test
    void metadataFor_carriesActorId() {
        // Arrange
        UUID actorId = UUID.randomUUID();
        WorldEvent event = WorldEvent.builder()
                .sequence(1L)
                .gameTick(100L)
                .type(WorldEventType.RESOURCE_HARVESTED)
                .actorId(actorId)
                .posX(0).posY(64).posZ(0)
                .chunkX(0).chunkZ(0)
                .build();
        Observation observation = ObservationFactory.fromEvent(event, CURRENT_TICK);

        // Act
        Map<String, String> metadata = ObservationFactory.metadataFor(observation);

        // Assert
        assertEquals(actorId.toString(), metadata.get("actor_id"));
    }

    @Test
    void metadataFor_carriesEventMetaWhenPresent() {
        // Arrange — behavior events carry a behavior key string as metadata
        WorldEvent event = WorldEvent.builder()
                .sequence(1L)
                .gameTick(100L)
                .type(WorldEventType.BEHAVIOR_STARTED)
                .actorId(UUID.randomUUID())
                .posX(0).posY(64).posZ(0)
                .chunkX(0).chunkZ(0)
                .metadata("harvest_melon")
                .build();
        Observation observation = ObservationFactory.fromEvent(event, CURRENT_TICK);

        // Act
        Map<String, String> metadata = ObservationFactory.metadataFor(observation);

        // Assert
        assertEquals("harvest_melon", metadata.get("event_meta"));
    }

    @Test
    void metadataFor_doesNotCarryPositionKeys() {
        // Arrange — position is no longer written into the metadata map; it is packed by the
        // caller (PerceptionPipeline) into a typed field instead (B1).
        WorldEvent event = WorldEvent.builder()
                .sequence(1L)
                .gameTick(100L)
                .type(WorldEventType.RESOURCE_HARVESTED)
                .actorId(UUID.randomUUID())
                .posX(12.5).posY(64.0).posZ(-8.25)
                .chunkX(0).chunkZ(0)
                .build();
        Observation observation = ObservationFactory.fromEvent(event, CURRENT_TICK);

        // Act
        Map<String, String> metadata = ObservationFactory.metadataFor(observation);

        // Assert
        assertNull(metadata.get("pos_x"));
        assertNull(metadata.get("pos_y"));
        assertNull(metadata.get("pos_z"));
    }

    @Test
    void fromEvent_sameWorldEventProducesStableObservationId() {
        // Arrange
        WorldEvent event = worldEvent(WorldEventType.RESOURCE_HARVESTED);

        // Act
        Observation firstObservation = ObservationFactory.fromEvent(event, CURRENT_TICK);
        Observation secondObservation = ObservationFactory.fromEvent(event, CURRENT_TICK + 1L);

        // Assert
        assertEquals(firstObservation.id(), secondObservation.id());
    }

    @Test
    void fromEvent_differentSequencesProduceDifferentObservationIds() {
        // Arrange
        UUID actorId = UUID.randomUUID();
        WorldEvent firstEvent = worldEvent(WorldEventType.RESOURCE_HARVESTED, actorId, 1L, 100L);
        WorldEvent secondEvent = worldEvent(WorldEventType.RESOURCE_HARVESTED, actorId, 2L, 100L);

        // Act
        Observation firstObservation = ObservationFactory.fromEvent(firstEvent, CURRENT_TICK);
        Observation secondObservation = ObservationFactory.fromEvent(secondEvent, CURRENT_TICK);

        // Assert
        assertNotEquals(firstObservation.id(), secondObservation.id());
    }

    @Test
    void fromEvent_sameActorAndSequenceAtDifferentGameTicksProduceDifferentObservationIds() {
        // Arrange
        UUID actorId = UUID.randomUUID();
        WorldEvent beforeReloadEvent = worldEvent(WorldEventType.RESOURCE_HARVESTED, actorId, 1L, 100L);
        WorldEvent afterReloadEvent = worldEvent(WorldEventType.RESOURCE_HARVESTED, actorId, 1L, 200L);

        // Act
        Observation beforeReloadObservation = ObservationFactory.fromEvent(beforeReloadEvent, CURRENT_TICK);
        Observation afterReloadObservation = ObservationFactory.fromEvent(afterReloadEvent, CURRENT_TICK);

        // Assert
        assertNotEquals(beforeReloadObservation.id(), afterReloadObservation.id());
    }

    @Test
    void fromEvent_relatedEntityIsNullWhenEventHasNoTarget() {
        // Arrange
        WorldEvent event = worldEvent(WorldEventType.RESOURCE_HARVESTED);

        // Act
        Observation observation = ObservationFactory.fromEvent(event, CURRENT_TICK);

        // Assert
        assertNotNull(observation); // sanity
        assertEquals(null, observation.relatedEntity());
    }

    @Test
    void fromEvent_relatedEntityCarriesTargetWhenPresent() {
        // Arrange
        UUID targetId = UUID.randomUUID();
        WorldEvent event = WorldEvent.builder()
                .sequence(1L)
                .gameTick(100L)
                .type(WorldEventType.TRADE_INVITE_SENT)
                .actorId(UUID.randomUUID())
                .targetId(targetId)
                .posX(0).posY(64).posZ(0)
                .chunkX(0).chunkZ(0)
                .build();

        // Act
        Observation observation = ObservationFactory.fromEvent(event, CURRENT_TICK);

        // Assert
        assertEquals(targetId, observation.relatedEntity());
    }

    @Test
    void fromEvent_propagatesOutcomeOntoObservation() {
        // Arrange
        WorldEvent event = WorldEvent.builder()
                .sequence(1L)
                .gameTick(100L)
                .type(WorldEventType.COURTSHIP_CHILD_BIRTH)
                .actorId(UUID.randomUUID())
                .posX(0).posY(64).posZ(0)
                .chunkX(0).chunkZ(0)
                .outcome(EventOutcome.FAILURE)
                .build();

        // Act
        Observation observation = ObservationFactory.fromEvent(event, CURRENT_TICK);

        // Assert
        assertEquals(EventOutcome.FAILURE, observation.outcome());
    }

    @Test
    void fromEvent_propagatesReasonOntoObservation() {
        // Arrange
        WorldEvent event = WorldEvent.builder()
                .sequence(1L)
                .gameTick(100L)
                .type(WorldEventType.COURTSHIP_CHILD_BIRTH)
                .actorId(UUID.randomUUID())
                .posX(0).posY(64).posZ(0)
                .chunkX(0).chunkZ(0)
                .reason("no bed available")
                .build();

        // Act
        Observation observation = ObservationFactory.fromEvent(event, CURRENT_TICK);

        // Assert
        assertEquals("no bed available", observation.reason());
    }

    @Test
    void fromEvent_propagatesDetailOntoObservation() {
        // Arrange
        WorldEvent event = WorldEvent.builder()
                .sequence(1L)
                .gameTick(100L)
                .type(WorldEventType.RESOURCE_HARVESTED)
                .actorId(UUID.randomUUID())
                .posX(0).posY(64).posZ(0)
                .chunkX(0).chunkZ(0)
                .detail("3 melons")
                .build();

        // Act
        Observation observation = ObservationFactory.fromEvent(event, CURRENT_TICK);

        // Assert
        assertEquals("3 melons", observation.detail());
    }

    @Test
    void fromEvent_absenceOfNewFields_leavesNullsOnObservation() {
        // Arrange — a plain event without outcome/reason/detail
        WorldEvent event = worldEvent(WorldEventType.TRADE_COMPLETED);

        // Act
        Observation observation = ObservationFactory.fromEvent(event, CURRENT_TICK);

        // Assert — nulls propagate, not defaults
        assertNull(observation.outcome());
        assertNull(observation.reason());
        assertNull(observation.detail());
    }

    @Test
    void metadataFor_writesOutcomeKeyWhenPresent() {
        // Arrange
        WorldEvent event = WorldEvent.builder()
                .sequence(1L)
                .gameTick(100L)
                .type(WorldEventType.TRADE_COMPLETED)
                .actorId(UUID.randomUUID())
                .posX(0).posY(64).posZ(0)
                .chunkX(0).chunkZ(0)
                .outcome(EventOutcome.FAILURE)
                .build();
        Observation observation = ObservationFactory.fromEvent(event, CURRENT_TICK);

        // Act
        Map<String, String> metadata = ObservationFactory.metadataFor(observation);

        // Assert
        assertEquals(EventOutcome.FAILURE.name(), metadata.get(ObservationMetadataKeys.OUTCOME));
    }

    @Test
    void metadataFor_omitsOutcomeKeyWhenAbsent() {
        // Arrange — no outcome on the event
        WorldEvent event = worldEvent(WorldEventType.TRADE_COMPLETED);
        Observation observation = ObservationFactory.fromEvent(event, CURRENT_TICK);

        // Act
        Map<String, String> metadata = ObservationFactory.metadataFor(observation);

        // Assert — key must be absent, not present with a null/empty value
        assertNull(metadata.get(ObservationMetadataKeys.OUTCOME));
    }

    @Test
    void metadataFor_omitsOutcomeKeyWhenSuccess() {
        // Arrange — SUCCESS is the default interpretation downstream (B4); do not persist it
        WorldEvent event = WorldEvent.builder()
                .sequence(1L)
                .gameTick(100L)
                .type(WorldEventType.TRADE_COMPLETED)
                .actorId(UUID.randomUUID())
                .posX(0).posY(64).posZ(0)
                .chunkX(0).chunkZ(0)
                .outcome(EventOutcome.SUCCESS)
                .build();
        Observation observation = ObservationFactory.fromEvent(event, CURRENT_TICK);

        // Act
        Map<String, String> metadata = ObservationFactory.metadataFor(observation);

        // Assert
        assertNull(metadata.get(ObservationMetadataKeys.OUTCOME));
    }

    @Test
    void metadataFor_writesReasonKeyWhenPresent() {
        // Arrange
        WorldEvent event = WorldEvent.builder()
                .sequence(1L)
                .gameTick(100L)
                .type(WorldEventType.COURTSHIP_CHILD_BIRTH)
                .actorId(UUID.randomUUID())
                .posX(0).posY(64).posZ(0)
                .chunkX(0).chunkZ(0)
                .reason("target already paired")
                .build();
        Observation observation = ObservationFactory.fromEvent(event, CURRENT_TICK);

        // Act
        Map<String, String> metadata = ObservationFactory.metadataFor(observation);

        // Assert
        assertEquals("target already paired", metadata.get(ObservationMetadataKeys.REASON));
    }

    @Test
    void metadataFor_omitsReasonKeyWhenAbsent() {
        // Arrange
        WorldEvent event = worldEvent(WorldEventType.COURTSHIP_CHILD_BIRTH);
        Observation observation = ObservationFactory.fromEvent(event, CURRENT_TICK);

        // Act
        Map<String, String> metadata = ObservationFactory.metadataFor(observation);

        // Assert
        assertNull(metadata.get(ObservationMetadataKeys.REASON));
    }

    @Test
    void metadataFor_writesDetailKeyWhenPresent() {
        // Arrange
        WorldEvent event = WorldEvent.builder()
                .sequence(1L)
                .gameTick(100L)
                .type(WorldEventType.RESOURCE_HARVESTED)
                .actorId(UUID.randomUUID())
                .posX(0).posY(64).posZ(0)
                .chunkX(0).chunkZ(0)
                .detail("3 melons")
                .build();
        Observation observation = ObservationFactory.fromEvent(event, CURRENT_TICK);

        // Act
        Map<String, String> metadata = ObservationFactory.metadataFor(observation);

        // Assert
        assertEquals("3 melons", metadata.get(ObservationMetadataKeys.DETAIL));
    }

    @Test
    void metadataFor_omitsDetailKeyWhenAbsent() {
        // Arrange
        WorldEvent event = worldEvent(WorldEventType.RESOURCE_HARVESTED);
        Observation observation = ObservationFactory.fromEvent(event, CURRENT_TICK);

        // Act
        Map<String, String> metadata = ObservationFactory.metadataFor(observation);

        // Assert
        assertNull(metadata.get(ObservationMetadataKeys.DETAIL));
    }

    @Test
    void fromEvent_whenDedupeKeyPresent_usesItAsObservationId() {
        // Arrange — a sighting event carries a content-addressed dedupeKey
        UUID dedupeKey = UUID.randomUUID();
        WorldEvent event = WorldEvent.builder()
                .sequence(1L)
                .gameTick(100L)
                .type(WorldEventType.ZOMBIE_SIGHTED)
                .actorId(UUID.randomUUID())
                .posX(10).posY(64).posZ(-5)
                .chunkX(0).chunkZ(0)
                .dedupeKey(dedupeKey)
                .build();

        // Act
        Observation observation = ObservationFactory.fromEvent(event, CURRENT_TICK);

        // Assert — observation id must be the dedupeKey, so co-witnesses converge
        assertEquals(dedupeKey, observation.id(),
                "When dedupeKey is present, observation id must equal it directly");
    }

    @Test
    void fromEvent_twoEventsWithSameDedupeKey_produceTheSameObservationId() {
        // Arrange — two villagers independently witness the same zombie; same dedupeKey
        UUID dedupeKey = UUID.randomUUID();

        WorldEvent witnessA = WorldEvent.builder()
                .sequence(1L).gameTick(100L)
                .type(WorldEventType.ZOMBIE_SIGHTED)
                .actorId(UUID.randomUUID())
                .posX(10).posY(64).posZ(-5)
                .chunkX(0).chunkZ(0)
                .dedupeKey(dedupeKey)
                .build();

        WorldEvent witnessB = WorldEvent.builder()
                .sequence(2L).gameTick(100L)
                .type(WorldEventType.ZOMBIE_SIGHTED)
                .actorId(UUID.randomUUID())
                .posX(10).posY(64).posZ(-5)
                .chunkX(0).chunkZ(0)
                .dedupeKey(dedupeKey)
                .build();

        // Act
        UUID idA = ObservationFactory.fromEvent(witnessA, CURRENT_TICK).id();
        UUID idB = ObservationFactory.fromEvent(witnessB, CURRENT_TICK).id();

        // Assert — both witnesses produce the same observation id, enabling corroboration
        assertEquals(idA, idB, "Co-witnesses with the same dedupeKey must produce the same observation id");
    }

    @Test
    void fromEvent_whenDedupeKeyAbsent_fallsBackToActorKeyedDerivation() {
        // Arrange — a normal (non-sighting) event with no dedupeKey
        UUID actorId = UUID.randomUUID();
        long sequence = 3L;
        long gameTick = 200L;

        WorldEvent event = WorldEvent.builder()
                .sequence(sequence)
                .gameTick(gameTick)
                .type(WorldEventType.RESOURCE_HARVESTED)
                .actorId(actorId)
                .posX(0).posY(64).posZ(0)
                .chunkX(0).chunkZ(0)
                .build();

        // Act
        UUID observationId = ObservationFactory.fromEvent(event, CURRENT_TICK).id();

        // Assert — must match the pre-existing actor-keyed formula exactly
        long actorMost = actorId.getMostSignificantBits();
        long actorLeast = actorId.getLeastSignificantBits();
        UUID expected = new UUID(
                actorMost ^ Long.rotateLeft(gameTick, 32),
                actorLeast ^ gameTick ^ sequence);

        assertEquals(expected, observationId,
                "Absent dedupeKey must fall back to the existing actor-keyed derivation unchanged");
    }

    private static WorldEvent worldEvent(WorldEventType type) {
        return worldEvent(type, UUID.randomUUID(), 1L);
    }

    private static WorldEvent worldEvent(WorldEventType type, UUID actorId, long sequence) {
        return worldEvent(type, actorId, sequence, 100L);
    }

    private static WorldEvent worldEvent(WorldEventType type, UUID actorId, long sequence, long gameTick) {
        return WorldEvent.builder()
                .sequence(sequence)
                .gameTick(gameTick)
                .type(type)
                .actorId(actorId)
                .posX(0).posY(64).posZ(0)
                .chunkX(0).chunkZ(0)
                .build();
    }

}
