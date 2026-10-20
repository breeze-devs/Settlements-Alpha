package dev.breezes.settlements.infrastructure.network.features.arrival.packet;

import dev.breezes.settlements.infrastructure.minecraft.event.settlement.SettlementRegionCause;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettlementAnnouncementTypeTest {

    @Test
    void ofEntry_isFounded_whenTheSettlementWasFoundedAroundThePlayer() {
        // Act
        SettlementAnnouncementType moment = SettlementAnnouncementType.ofEntry(SettlementRegionCause.FOUNDED, false);

        // Assert: a founding is its own title, never also the settlement's discovery
        assertEquals(SettlementAnnouncementType.FOUNDED, moment);
    }

    @Test
    void ofEntry_isDiscovery_forTheFirstEntry_whetherThePlayerMovedOrARecordAppearedAround() {
        // Act
        SettlementAnnouncementType walkedIn = SettlementAnnouncementType.ofEntry(SettlementRegionCause.MOVED, false);
        SettlementAnnouncementType recordedAround = SettlementAnnouncementType.ofEntry(SettlementRegionCause.RECORDED, false);

        // Assert
        assertEquals(SettlementAnnouncementType.DISCOVERY, walkedIn);
        assertEquals(SettlementAnnouncementType.DISCOVERY, recordedAround);
    }

    @Test
    void ofEntry_isEntry_forASettlementDiscoveredBefore() {
        // Act
        SettlementAnnouncementType walkedIn = SettlementAnnouncementType.ofEntry(SettlementRegionCause.MOVED, true);
        SettlementAnnouncementType recordedAround = SettlementAnnouncementType.ofEntry(SettlementRegionCause.RECORDED, true);

        // Assert
        assertEquals(SettlementAnnouncementType.ENTRY, walkedIn);
        assertEquals(SettlementAnnouncementType.ENTRY, recordedAround);
    }

    @Test
    void ofExit_isDisbanded_whenTheSettlementDisbanded() {
        // Act
        SettlementAnnouncementType moment = SettlementAnnouncementType.ofExit(SettlementRegionCause.DISBANDED);

        // Assert
        assertEquals(SettlementAnnouncementType.DISBANDED, moment);
    }

    @Test
    void ofExit_isExit_whenThePlayerMovedOut() {
        // Act
        SettlementAnnouncementType moment = SettlementAnnouncementType.ofExit(SettlementRegionCause.MOVED);

        // Assert
        assertEquals(SettlementAnnouncementType.EXIT, moment);
    }

}
