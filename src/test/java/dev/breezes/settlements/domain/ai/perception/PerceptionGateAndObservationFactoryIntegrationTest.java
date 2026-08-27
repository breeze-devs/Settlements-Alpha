package dev.breezes.settlements.domain.ai.perception;

import dev.breezes.settlements.application.ai.socialcue.SocialCueRuntimeState;
import dev.breezes.settlements.domain.ai.observation.ObservationBuffer;
import dev.breezes.settlements.domain.ai.worldevent.WorldEvent;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventBus;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration-style test covering the gate + factory + buffer + cursor pipeline in plain Java.
 * <p>
 * Verifies the core anti-telepathy invariant: two "villagers" at different chunk positions
 * observing the same set of events end up with different knowledge.
 */
class PerceptionGateAndObservationFactoryIntegrationTest {

    private static final long GAME_TICK = 1000L;

    private WorldEventBus bus;

    @BeforeEach
    void setUp() {
        this.bus = new WorldEventBus();
    }

    @Test
    void nearbyVillagerAdmitsEvent_distantVillagerDoesNot() {
        // Arrange
        WorldEvent event = emitEvent(WorldEventType.RESOURCE_HARVESTED, 5, 5);

        // Villager A is in chunk (5, 5) — right next to the event
        int nearbyChunkX = 5, nearbyChunkZ = 5;
        // Villager B is in chunk (20, 20) — far away
        int farChunkX = 20, farChunkZ = 20;

        // Act
        boolean nearbyAdmits = PerceptionGate.admits(event, nearbyChunkX, nearbyChunkZ);
        boolean distantAdmits = PerceptionGate.admits(event, farChunkX, farChunkZ);

        // Assert
        assertTrue(nearbyAdmits, "nearby villager should perceive the event");
        assertFalse(distantAdmits, "distant villager should not perceive the event");
    }

    @Test
    void bothVillagersObservingSameEvent_produceDifferentObservationCounts() {
        // Arrange — emit one event at chunk (10, 10)
        emitEvent(WorldEventType.SHEEP_SHEARED, 10, 10);

        // Villager A: chunk (10, 10) — within radius
        // Villager B: chunk (20, 20) — outside radius
        List<WorldEvent> delta = visitDelta(0L);

        ObservationBuffer nearbyBuffer = new ObservationBuffer();
        ObservationBuffer distantBuffer = new ObservationBuffer();

        for (WorldEvent event : delta) {
            if (PerceptionGate.admits(event, 10, 10)) {
                nearbyBuffer.add(ObservationFactory.fromEvent(event, GAME_TICK));
            }
            if (PerceptionGate.admits(event, 20, 20)) {
                distantBuffer.add(ObservationFactory.fromEvent(event, GAME_TICK));
            }
        }

        // Assert
        assertEquals(1, nearbyBuffer.size(), "nearby villager should have one observation");
        assertEquals(0, distantBuffer.size(), "distant villager should have no observations");
    }

    @Test
    void cursorAdvances_afterProcessingDelta() {
        // Arrange
        SocialCueRuntimeState state = new SocialCueRuntimeState();
        assertEquals(0L, state.getLastSeenSeq(), "cursor starts at 0");

        emitEvent(WorldEventType.RESOURCE_HARVESTED, 10, 10);
        emitEvent(WorldEventType.SHEEP_SHEARED, 10, 10);
        List<WorldEvent> delta = visitDelta(state.getLastSeenSeq());

        // Act — simulate what PerceptionPipeline does with the cursor
        long newSeq = delta.get(delta.size() - 1).getSequence();
        state.advanceCursor(newSeq);

        // Assert — cursor now points past the last processed event
        assertTrue(state.getLastSeenSeq() >= 2L, "cursor should have advanced past both events");
        // Subsequent delta should be empty
        List<WorldEvent> nextDelta = visitDelta(state.getLastSeenSeq());
        assertTrue(nextDelta.isEmpty(), "no new events should be visible after cursor advance");
    }

    private WorldEvent emitEvent(WorldEventType type, int chunkX, int chunkZ) {
        return this.bus.emit(
                WorldEvent.builder()
                        .type(type)
                        .actorId(UUID.randomUUID())
                        .posX(chunkX * 16.0)
                        .posY(64.0)
                        .posZ(chunkZ * 16.0)
                        .chunkX(chunkX)
                        .chunkZ(chunkZ),
                GAME_TICK);
    }

    private List<WorldEvent> visitDelta(long lastSeenSeq) {
        List<WorldEvent> visited = new ArrayList<>();
        this.bus.visitDelta(lastSeenSeq, visited::add);
        return visited;
    }

}
