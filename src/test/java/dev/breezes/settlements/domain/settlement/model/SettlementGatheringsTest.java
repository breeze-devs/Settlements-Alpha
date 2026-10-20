package dev.breezes.settlements.domain.settlement.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SettlementGatheringsTest {

    @Test
    void rejectsALastDay_whenNoGatheringWasCounted() {
        // Act, Assert
        assertThrows(IllegalArgumentException.class, () -> new SettlementGatherings(0, 5L));
    }

    @Test
    void rejectsAMissingLastDay_whenGatheringsWereCounted() {
        // Act, Assert
        assertThrows(IllegalArgumentException.class, () -> new SettlementGatherings(2, SettlementGatherings.NO_DAY));
    }

    @Test
    void checkIn_countsTheFirstGathering_onDayZero() {
        // Act
        SettlementGatherings checkedIn = SettlementGatherings.NONE.checkIn(0L);

        // Assert
        assertEquals(new SettlementGatherings(1, 0L), checkedIn);
    }

    @Test
    void checkIn_returnsTheSameTally_onTheLastGatheringDay() {
        // Arrange
        SettlementGatherings gatherings = new SettlementGatherings(4, 17L);

        // Act
        SettlementGatherings checkedIn = gatherings.checkIn(17L);

        // Assert
        assertSame(gatherings, checkedIn);
    }

    @Test
    void checkIn_countsAGathering_onALaterDay() {
        // Act
        SettlementGatherings checkedIn = new SettlementGatherings(4, 17L).checkIn(18L);

        // Assert
        assertEquals(new SettlementGatherings(5, 18L), checkedIn);
    }

    @Test
    void checkIn_countsAGathering_onAnEarlierDay_afterTheCalendarIsSetBack() {
        // Arrange — the world's time was set back, so the calendar reads a day before the last gathering
        SettlementGatherings gatherings = new SettlementGatherings(4, 17L);

        // Act
        SettlementGatherings checkedIn = gatherings.checkIn(3L);

        // Assert
        assertEquals(new SettlementGatherings(5, 3L), checkedIn);
    }

}
