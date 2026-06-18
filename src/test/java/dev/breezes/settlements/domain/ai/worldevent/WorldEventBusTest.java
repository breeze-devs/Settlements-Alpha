package dev.breezes.settlements.domain.ai.worldevent;

import dev.breezes.settlements.domain.ai.eventlane.EventLaneConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldEventBusTest {

    private WorldEventBus bus;

    @BeforeEach
    void setUp() {
        this.bus = new WorldEventBus();
    }

    // -------------------------------------------------------------------------
    // Append + visitDelta
    // -------------------------------------------------------------------------

    @Test
    void emit_assignsMonotonicSeq() {
        // Arrange
        UUID actor = UUID.randomUUID();

        // Act
        WorldEvent first = bus.emit(buildBuilder(actor, WorldEventType.BEHAVIOR_STARTED), 100L);
        WorldEvent second = bus.emit(buildBuilder(actor, WorldEventType.BEHAVIOR_COMPLETED), 101L);

        // Assert
        assertTrue(first.getSequence() < second.getSequence());
    }

    @Test
    void visitDelta_visitsOnlyEventsAfterCursor() {
        // Arrange
        UUID actor = UUID.randomUUID();
        WorldEvent e1 = bus.emit(buildBuilder(actor, WorldEventType.BEHAVIOR_STARTED), 100L);
        WorldEvent e2 = bus.emit(buildBuilder(actor, WorldEventType.SHEEP_SHEARED), 101L);
        WorldEvent e3 = bus.emit(buildBuilder(actor, WorldEventType.CROP_HARVESTED), 102L);
        List<WorldEvent> visited = new ArrayList<>();

        // Act — cursor set past e1, should return e2 and e3
        bus.visitDelta(e1.getSequence(), visited::add);

        // Assert
        assertEquals(2, visited.size());
        assertEquals(e2.getSequence(), visited.get(0).getSequence());
        assertEquals(e3.getSequence(), visited.get(1).getSequence());
    }

    @Test
    void visitDelta_visitsAllEventsWhenCursorIsZero() {
        // Arrange
        UUID actor = UUID.randomUUID();
        bus.emit(buildBuilder(actor, WorldEventType.BEHAVIOR_STARTED), 100L);
        bus.emit(buildBuilder(actor, WorldEventType.BEHAVIOR_COMPLETED), 101L);
        List<WorldEvent> visited = new ArrayList<>();

        // Act
        bus.visitDelta(0L, visited::add);

        // Assert
        assertEquals(2, visited.size());
    }

    @Test
    void visitDelta_visitsNothing_whenNothingNew() {
        // Arrange
        UUID actor = UUID.randomUUID();
        WorldEvent event = bus.emit(buildBuilder(actor, WorldEventType.BEHAVIOR_STARTED), 100L);
        List<WorldEvent> visited = new ArrayList<>();

        // Act
        bus.visitDelta(event.getSequence(), visited::add);

        // Assert
        assertTrue(visited.isEmpty());
    }

    @Test
    void visitDelta_visitsNothingForFreshBus() {
        // Arrange
        List<WorldEvent> visited = new ArrayList<>();

        // Act
        bus.visitDelta(0L, visited::add);

        // Assert
        assertTrue(visited.isEmpty());
    }

    @Test
    void visitDelta_visitsOnlyEventsAfterCursorAndReturnsHighestSeq() {
        // Arrange
        UUID actor = UUID.randomUUID();
        WorldEvent e1 = bus.emit(buildBuilder(actor, WorldEventType.BEHAVIOR_STARTED), 100L);
        WorldEvent e2 = bus.emit(buildBuilder(actor, WorldEventType.SHEEP_SHEARED), 101L);
        WorldEvent e3 = bus.emit(buildBuilder(actor, WorldEventType.CROP_HARVESTED), 102L);
        List<WorldEvent> visited = new ArrayList<>();

        // Act
        long highestSeq = bus.visitDelta(e1.getSequence(), visited::add);

        // Assert
        assertEquals(e3.getSequence(), highestSeq);
        assertEquals(List.of(e2, e3), visited);
    }

    @Test
    void visitDelta_returnsCursorWhenNothingNew() {
        // Arrange
        UUID actor = UUID.randomUUID();
        WorldEvent event = bus.emit(buildBuilder(actor, WorldEventType.BEHAVIOR_STARTED), 100L);
        List<WorldEvent> visited = new ArrayList<>();

        // Act
        long highestSeq = bus.visitDelta(event.getSequence(), visited::add);

        // Assert
        assertEquals(event.getSequence(), highestSeq);
        assertTrue(visited.isEmpty());
    }

    // -------------------------------------------------------------------------
    // Eviction
    // -------------------------------------------------------------------------

    @Test
    void evict_removesOldEvents_retainsRecent() {
        // Arrange
        UUID actor = UUID.randomUUID();
        bus.emit(buildBuilder(actor, WorldEventType.BEHAVIOR_STARTED), 10L);   // old
        bus.emit(buildBuilder(actor, WorldEventType.BEHAVIOR_COMPLETED), 105L); // recent

        // Act — evict at tick 115, TTL is 100; events at tick < 15 are evicted
        bus.evict(115L);

        // Assert — the event at tick 10 is gone (115 - 100 = 15 > 10), recent stays
        List<WorldEvent> remaining = bus.snapshotLog();
        assertEquals(1, remaining.size());
        assertEquals(105L, remaining.get(0).getGameTick());
    }

    @Test
    void evict_retainsAllWhenAllRecent() {
        // Arrange
        UUID actor = UUID.randomUUID();
        bus.emit(buildBuilder(actor, WorldEventType.BEHAVIOR_STARTED), 100L);
        bus.emit(buildBuilder(actor, WorldEventType.BEHAVIOR_COMPLETED), 101L);

        // Act — evict at tick 150; threshold = 150 - 100 = 50; both events at 100+, retained
        bus.evict(150L);

        // Assert
        assertEquals(2, bus.logSize());
    }

    @Test
    void evict_usesConfiguredTtl() {
        // Arrange
        UUID actor = UUID.randomUUID();
        WorldEventBus configuredBus = new WorldEventBus(eventLaneConfigWithTtl(40));
        configuredBus.emit(buildBuilder(actor, WorldEventType.BEHAVIOR_STARTED), 100L);
        configuredBus.emit(buildBuilder(actor, WorldEventType.BEHAVIOR_COMPLETED), 130L);

        // Act — threshold = 150 - 40 = 110, so only the first event is evicted.
        configuredBus.evict(150L);

        // Assert
        List<WorldEvent> remaining = configuredBus.snapshotLog();
        assertEquals(1, remaining.size());
        assertEquals(130L, remaining.get(0).getGameTick());
    }

    // -------------------------------------------------------------------------
    // currentSeq
    // -------------------------------------------------------------------------

    @Test
    void currentSeq_returnsZeroForEmptyBus() {
        assertEquals(0L, bus.currentSeq());
    }

    @Test
    void currentSeq_returnsLastEmittedSeq() {
        // Arrange
        UUID actor = UUID.randomUUID();
        WorldEvent event = bus.emit(buildBuilder(actor, WorldEventType.BEHAVIOR_STARTED), 100L);

        // Act & Assert
        assertEquals(event.getSequence(), bus.currentSeq());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static WorldEvent.WorldEventBuilder buildBuilder(UUID actorId, WorldEventType type) {
        return WorldEvent.fromPos(0, 64, 0)
                .type(type)
                .actorId(actorId);
    }

    private static EventLaneConfig eventLaneConfigWithTtl(int ttlTicks) {
        return new EventLaneConfig(ttlTicks, 50, 200, 25,
                120, 120, 10, 300, 4.0, 0.5, 0.25, "exponential", 9.627044E-6f);
    }

}
