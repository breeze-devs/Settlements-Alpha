package dev.breezes.settlements.domain.settlement.model;

import org.junit.jupiter.api.Test;

import static dev.breezes.settlements.domain.settlement.model.SettlementFixtures.logEntry;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SettlementLogEntryTest {

    @Test
    void rejectsASubjectName_withoutTheSubjectItNames() {
        // Arrange
        SettlementLogEntry.SettlementLogEntryBuilder entry = logEntry(SettlementLogType.DIED).subjectName("Alder");

        // Act, Assert
        assertThrows(IllegalArgumentException.class, entry::build);
    }

}
