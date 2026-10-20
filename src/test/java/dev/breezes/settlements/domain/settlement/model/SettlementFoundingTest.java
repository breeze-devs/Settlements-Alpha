package dev.breezes.settlements.domain.settlement.model;

import dev.breezes.settlements.domain.world.CivilDateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementFoundingTest {

    @Test
    void isEstablished_whenFoundedBeforeTheWorldAndItsRecord() {
        // Arrange
        SettlementFounding founding = new SettlementFounding(new CivilDateTime(-480_000L), new CivilDateTime(24_000L));

        // Act, Assert
        assertTrue(founding.isEstablished());
    }

    @Test
    void isNotEstablished_whenFoundedAsItsRecordBegan() {
        // Arrange
        SettlementFounding founding = new SettlementFounding(new CivilDateTime(24_000L), new CivilDateTime(24_000L));

        // Act, Assert
        assertFalse(founding.isEstablished());
    }

    @Test
    void rejectsAFounding_afterItsRecordBegan() {
        // Arrange
        CivilDateTime founded = new CivilDateTime(24_001L);
        CivilDateTime recorded = new CivilDateTime(24_000L);

        // Act, Assert
        assertThrows(IllegalArgumentException.class, () -> new SettlementFounding(founded, recorded));
    }

}
