package dev.breezes.settlements.domain.ai.perception;

import dev.breezes.settlements.domain.ai.worldevent.WorldEvent;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventNamespace;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerceptionGateTest {

    private static final int VILLAGER_CHUNK_X = 10;
    private static final int VILLAGER_CHUNK_Z = 10;

    // -------------------------------------------------------------------------
    // Namespace rejection
    // -------------------------------------------------------------------------

    @Test
    void admits_rejectsSystemNamespaceEventRegardlessOfDistance() {
        // Arrange — system event right next to the villager (same chunk)
        WorldEvent event = worldEvent(WorldEventType.DAY_PLAN_INVALIDATED, VILLAGER_CHUNK_X, VILLAGER_CHUNK_Z);

        // Act & Assert
        assertFalse(PerceptionGate.admits(event, VILLAGER_CHUNK_X, VILLAGER_CHUNK_Z));
    }

    @Test
    void admits_rejectsPlanExhaustedSystemEvent() {
        // Arrange
        WorldEvent event = worldEvent(WorldEventType.PLAN_EXHAUSTED, VILLAGER_CHUNK_X, VILLAGER_CHUNK_Z);

        // Act & Assert
        assertFalse(PerceptionGate.admits(event, VILLAGER_CHUNK_X, VILLAGER_CHUNK_Z));
    }

    // -------------------------------------------------------------------------
    // Manhattan distance checks
    // -------------------------------------------------------------------------

    @Test
    void admits_admitsWorldEventInSameChunk() {
        // Arrange
        WorldEvent event = worldEvent(WorldEventType.CROP_HARVESTED, VILLAGER_CHUNK_X, VILLAGER_CHUNK_Z);

        // Act & Assert
        assertTrue(PerceptionGate.admits(event, VILLAGER_CHUNK_X, VILLAGER_CHUNK_Z));
    }

    @Test
    void admits_admitsWorldEventAtExactMaxRadius() {
        // Arrange — Manhattan distance = MAX_PERCEPTION_CHUNK_RADIUS
        int eventChunkX = VILLAGER_CHUNK_X + PerceptionGate.MAX_PERCEPTION_CHUNK_RADIUS;
        WorldEvent event = worldEvent(WorldEventType.CROP_HARVESTED, eventChunkX, VILLAGER_CHUNK_Z);

        // Act & Assert
        assertTrue(PerceptionGate.admits(event, VILLAGER_CHUNK_X, VILLAGER_CHUNK_Z));
    }

    @Test
    void admits_rejectsWorldEventOneBeyondMaxRadius() {
        // Arrange — Manhattan distance = MAX_PERCEPTION_CHUNK_RADIUS + 1
        int eventChunkX = VILLAGER_CHUNK_X + PerceptionGate.MAX_PERCEPTION_CHUNK_RADIUS + 1;
        WorldEvent event = worldEvent(WorldEventType.CROP_HARVESTED, eventChunkX, VILLAGER_CHUNK_Z);

        // Act & Assert
        assertFalse(PerceptionGate.admits(event, VILLAGER_CHUNK_X, VILLAGER_CHUNK_Z));
    }

    @Test
    void admits_manhattanDistanceUsesBothAxes() {
        // Arrange — dx=2, dz=2 → Manhattan=4 = MAX_PERCEPTION_CHUNK_RADIUS
        WorldEvent atMax = worldEvent(WorldEventType.SHEEP_SHEARED,
                VILLAGER_CHUNK_X + 2, VILLAGER_CHUNK_Z + 2);
        // dx=2, dz=3 → Manhattan=5 → exceeds
        WorldEvent beyondMax = worldEvent(WorldEventType.SHEEP_SHEARED,
                VILLAGER_CHUNK_X + 2, VILLAGER_CHUNK_Z + 3);

        // Act & Assert
        assertTrue(PerceptionGate.admits(atMax, VILLAGER_CHUNK_X, VILLAGER_CHUNK_Z));
        assertFalse(PerceptionGate.admits(beyondMax, VILLAGER_CHUNK_X, VILLAGER_CHUNK_Z));
    }

    @Test
    void admits_worksBehindVillagerInNegativeDirection() {
        // Arrange — event at negative relative offset, still within radius
        int eventChunkX = VILLAGER_CHUNK_X - PerceptionGate.MAX_PERCEPTION_CHUNK_RADIUS;
        WorldEvent event = worldEvent(WorldEventType.TRADE_COMPLETED, eventChunkX, VILLAGER_CHUNK_Z);

        // Act & Assert
        assertTrue(PerceptionGate.admits(event, VILLAGER_CHUNK_X, VILLAGER_CHUNK_Z));
    }

    @Test
    void admits_rejectsDistantSocialEvent() {
        // Arrange — even a social event is rejected if it's too far away
        int eventChunkX = VILLAGER_CHUNK_X + 20;
        WorldEvent event = worldEvent(WorldEventType.COURTSHIP_COMPLETED, eventChunkX, VILLAGER_CHUNK_Z);

        // Act & Assert
        assertFalse(PerceptionGate.admits(event, VILLAGER_CHUNK_X, VILLAGER_CHUNK_Z));
    }

    @Test
    void admits_allowsAllWorldNamespaceTypesWithinRadius() {
        // Arrange & Act — all WORLD events at distance 0 should be admitted
        for (WorldEventType type : WorldEventType.values()) {
            if (type.getNamespace() != WorldEventNamespace.WORLD) {
                continue;
            }
            WorldEvent event = worldEvent(type, VILLAGER_CHUNK_X, VILLAGER_CHUNK_Z);

            // Assert
            assertTrue(PerceptionGate.admits(event, VILLAGER_CHUNK_X, VILLAGER_CHUNK_Z),
                    "Expected WORLD event " + type + " to be admitted at distance 0");
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static WorldEvent worldEvent(WorldEventType type, int chunkX, int chunkZ) {
        return WorldEvent.builder()
                .sequence(1L)
                .gameTick(100L)
                .type(type)
                .actorId(UUID.randomUUID())
                .posX(chunkX * 16.0)
                .posY(64.0)
                .posZ(chunkZ * 16.0)
                .chunkX(chunkX)
                .chunkZ(chunkZ)
                .build();
    }

}
