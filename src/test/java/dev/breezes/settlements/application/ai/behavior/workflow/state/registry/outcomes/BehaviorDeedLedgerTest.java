package dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes;

import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BehaviorDeedLedgerTest {

    @Test
    void declarePrimary_addsFirstEntryAsPrimary() {
        // Arrange
        BehaviorDeedLedger ledger = new BehaviorDeedLedger();
        BehaviorOutcome primary = BehaviorOutcome.forDeed(WorldEventType.ORE_SMELTED, null);

        // Act
        BehaviorOutcome declared = ledger.declarePrimary(primary);

        // Assert
        assertSame(primary, declared);
        assertSame(primary, ledger.primary().orElseThrow());
        assertEquals(List.of(primary), ledger.entriesView());
    }

    @Test
    void declarePrimary_replacesExistingPrimaryWithoutRemovingSecondaryEntries() {
        // Arrange
        BehaviorDeedLedger ledger = new BehaviorDeedLedger();
        BehaviorOutcome originalPrimary = BehaviorOutcome.forDeed(WorldEventType.ORE_SMELTED, null);
        BehaviorOutcome secondary = BehaviorOutcome.forDeed(WorldEventType.FURNACE_MISFIRED, null);
        BehaviorOutcome replacementPrimary = BehaviorOutcome.forDeed(WorldEventType.MEAT_SMOKED, null);
        ledger.declarePrimary(originalPrimary);
        ledger.addSecondary(secondary);

        // Act
        ledger.declarePrimary(replacementPrimary);

        // Assert
        assertEquals(List.of(replacementPrimary, secondary), ledger.entriesView());
    }

    @Test
    void addSecondary_appendsEntriesInOrder() {
        // Arrange
        BehaviorDeedLedger ledger = new BehaviorDeedLedger();
        BehaviorOutcome primary = BehaviorOutcome.forDeed(WorldEventType.CHICKENS_CHASED, null);
        BehaviorOutcome firstSecondary = BehaviorOutcome.forDeed(WorldEventType.CHICKENS_REVENGED, null);
        BehaviorOutcome secondSecondary = BehaviorOutcome.forDeed(WorldEventType.BELL_RUNG, null);
        ledger.declarePrimary(primary);

        // Act
        ledger.addSecondary(firstSecondary);
        ledger.addSecondary(secondSecondary);

        // Assert
        assertEquals(List.of(primary, firstSecondary, secondSecondary), ledger.entriesView());
    }

    @Test
    void addSecondary_dropsEntriesBeyondCap() {
        // Arrange
        BehaviorDeedLedger ledger = new BehaviorDeedLedger();
        BehaviorOutcome primary = BehaviorOutcome.forDeed(WorldEventType.CHICKENS_CHASED, null);
        BehaviorOutcome firstSecondary = BehaviorOutcome.forDeed(WorldEventType.CHICKENS_REVENGED, null);
        BehaviorOutcome secondSecondary = BehaviorOutcome.forDeed(WorldEventType.FURNACE_MISFIRED, null);
        BehaviorOutcome overCapSecondary = BehaviorOutcome.forDeed(WorldEventType.BELL_RUNG, null);
        ledger.declarePrimary(primary);
        ledger.addSecondary(firstSecondary);
        ledger.addSecondary(secondSecondary);

        // Act
        BehaviorOutcome returned = ledger.addSecondary(overCapSecondary);

        // Assert
        assertSame(overCapSecondary, returned);
        assertEquals(List.of(primary, firstSecondary, secondSecondary), ledger.entriesView());
    }

    @Test
    void entriesView_returnsImmutableSnapshot() {
        // Arrange
        BehaviorDeedLedger ledger = new BehaviorDeedLedger();
        ledger.declarePrimary(BehaviorOutcome.forDeed(WorldEventType.ORE_SMELTED, null));
        List<BehaviorOutcome> entries = ledger.entriesView();

        // Act, Assert
        assertThrows(UnsupportedOperationException.class,
                () -> entries.add(BehaviorOutcome.forDeed(WorldEventType.FURNACE_MISFIRED, null)));
    }

    @Test
    void reset_clearsAllEntries() {
        // Arrange
        BehaviorDeedLedger ledger = new BehaviorDeedLedger();
        ledger.declarePrimary(BehaviorOutcome.forDeed(WorldEventType.ORE_SMELTED, null));
        ledger.addSecondary(BehaviorOutcome.forDeed(WorldEventType.FURNACE_MISFIRED, null));

        // Act
        ledger.reset();

        // Assert
        assertTrue(ledger.primary().isEmpty());
        assertTrue(ledger.entriesView().isEmpty());
    }

}
