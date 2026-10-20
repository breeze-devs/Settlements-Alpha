package dev.breezes.settlements.domain.settlement.model;

import dev.breezes.settlements.domain.world.CivilDateTime;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

import static dev.breezes.settlements.domain.settlement.model.SettlementFixtures.logEntry;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SettlementLogTest {

    @Test
    void keepsEveryEntry_whenBuiltFromExactlyCapacityEntries() {
        // Arrange
        List<SettlementLogEntry> entries = entriesAtDayTimes(0, SettlementLog.CAPACITY);

        // Act
        SettlementLog log = new SettlementLog(entries);

        // Assert
        assertEquals(entries, log.entries());
    }

    @Test
    void dropsTheOldestEntries_whenBuiltFromMoreThanCapacityEntries() {
        // Arrange — one more entry than the log holds, day times counting up from the oldest
        List<SettlementLogEntry> entries = entriesAtDayTimes(0, SettlementLog.CAPACITY + 1);

        // Act
        SettlementLog log = new SettlementLog(entries);

        // Assert — the oldest entry is gone and the newest are kept, still oldest first
        assertEquals(entries.subList(1, entries.size()), log.entries());
    }

    @Test
    void append_addsTheEntryAsTheNewest() {
        // Arrange
        List<SettlementLogEntry> entries = entriesAtDayTimes(0, 2);
        SettlementLogEntry newest = logEntry(SettlementLogType.DIED).build();

        // Act
        SettlementLog log = new SettlementLog(entries).append(newest);

        // Assert
        assertEquals(List.of(entries.get(0), entries.get(1), newest), log.entries());
    }

    @Test
    void append_dropsTheOldestEntry_whenTheLogIsFull() {
        // Arrange
        List<SettlementLogEntry> entries = entriesAtDayTimes(0, SettlementLog.CAPACITY);
        SettlementLogEntry newest = logEntry(SettlementLogType.DIED).build();

        // Act
        SettlementLog log = new SettlementLog(entries).append(newest);

        // Assert — still full, without the oldest entry, and the new entry last
        List<SettlementLogEntry> expected = new ArrayList<>(entries.subList(1, entries.size()));
        expected.add(newest);
        assertEquals(expected, log.entries());
    }

    private static List<SettlementLogEntry> entriesAtDayTimes(long first, long count) {
        return LongStream.range(first, first + count)
                .mapToObj(dayTime -> logEntry(SettlementLogType.JOINED).date(new CivilDateTime(dayTime)).build())
                .toList();
    }

}
