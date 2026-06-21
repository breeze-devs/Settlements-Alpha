package dev.breezes.settlements.domain.ai.observation;

import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservationBufferTest {

    @Test
    void constructor_rejectsZeroCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new ObservationBuffer(0));
    }

    @Test
    void constructor_rejectsNegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new ObservationBuffer(-1));
    }

    @Test
    void add_storesObservationAndUpdatesSize() {
        ObservationBuffer buffer = new ObservationBuffer(3);
        Observation observation = observation(1L);

        buffer.add(observation);

        assertEquals(1, buffer.size());
        assertEquals(List.of(observation), buffer.peekRecent(1));
    }

    @Test
    void drain_returnsAllObservationsAndClearsBuffer() {
        ObservationBuffer buffer = new ObservationBuffer(3);
        Observation first = observation(1L);
        Observation second = observation(2L);
        buffer.add(first);
        buffer.add(second);

        List<Observation> drained = buffer.drain();

        assertEquals(List.of(first, second), drained);
        assertTrue(buffer.isEmpty());
    }

    @Test
    void drain_onEmptyBufferReturnsEmptyList() {
        ObservationBuffer buffer = new ObservationBuffer(3);

        List<Observation> drained = buffer.drain();

        assertTrue(drained.isEmpty());
    }

    @Test
    void overflow_evictsOldestObservation() {
        ObservationBuffer buffer = new ObservationBuffer(3);
        Observation first = observation(1L);
        Observation second = observation(2L);
        Observation third = observation(3L);
        Observation fourth = observation(4L);
        buffer.add(first);
        buffer.add(second);
        buffer.add(third);
        buffer.add(fourth);

        List<Observation> drained = buffer.drain();

        assertEquals(List.of(second, third, fourth), drained);
    }

    @Test
    void peekRecent_returnsReverseChronologicalObservations() {
        ObservationBuffer buffer = new ObservationBuffer(5);
        Observation first = observation(1L);
        Observation second = observation(2L);
        Observation third = observation(3L);
        Observation fourth = observation(4L);
        buffer.add(first);
        buffer.add(second);
        buffer.add(third);
        buffer.add(fourth);

        List<Observation> recent = buffer.peekRecent(3);

        assertEquals(List.of(fourth, third, second), recent);
    }

    @Test
    void peekRecent_withCountLargerThanSizeReturnsAllObservations() {
        ObservationBuffer buffer = new ObservationBuffer(5);
        Observation first = observation(1L);
        Observation second = observation(2L);
        buffer.add(first);
        buffer.add(second);

        List<Observation> recent = buffer.peekRecent(10);

        assertEquals(List.of(second, first), recent);
    }

    private static Observation observation(long timestamp) {
        return Observation.builder()
                .id(UUID.randomUUID())
                .timestampTick(timestamp)
                .type(ObservationType.ENVIRONMENT)
                .eventType(WorldEventType.RESOURCE_HARVESTED)
                .content("observation " + timestamp)
                .baseImportance(1.0F)
                .build();
    }

}
