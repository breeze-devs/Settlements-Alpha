package dev.breezes.settlements.domain.settlement.naming;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettlementRenameTest {

    @Test
    void renames_toTheRequestedName_whenNoOtherSettlementUsesIt() {
        // Act
        SettlementRename rename = SettlementRename.of("Ashford", "Millbrook", List.of("Ashford", "Oakwick"));

        // Assert
        assertEquals(new SettlementRename.Renamed("Ashford", "Millbrook"), rename);
    }

    @Test
    void renames_toTheRequestedNameWithANumeral_whenAnotherSettlementUsesIt() {
        // Act
        SettlementRename rename = SettlementRename.of("Ashford", "Millbrook", List.of("Ashford", "Millbrook"));

        // Assert
        assertEquals(new SettlementRename.Renamed("Ashford", "Millbrook II"), rename);
    }

    @Test
    void isUnchanged_whenTheRequestedNameIsItsOwn_becauseItsOwnNameIsNotTaken() {
        // Act — the level's names include the settlement's own, which must not push the request to a numeral
        SettlementRename rename = SettlementRename.of("Ashford", " Ashford ", List.of("Ashford", "Oakwick"));

        // Assert
        assertEquals(new SettlementRename.Unchanged("Ashford"), rename);
    }

    @Test
    void isUnchanged_whenTheNumeralForATakenNameIsTheOneItAlreadyHas() {
        // Arrange — it took its numeral because another settlement holds the plain name
        List<String> levelNames = List.of("Ashford", "Ashford II");

        // Act
        SettlementRename rename = SettlementRename.of("Ashford II", "Ashford", levelNames);

        // Assert
        assertEquals(new SettlementRename.Unchanged("Ashford II"), rename);
    }

    @Test
    void renames_whenOnlyTheCapitalizationDiffers() {
        // Act — case decides whether two names collide, never whether a rename changes the name
        SettlementRename rename = SettlementRename.of("ashford", "Ashford", List.of("ashford"));

        // Assert
        assertEquals(new SettlementRename.Renamed("ashford", "Ashford"), rename);
    }

}
