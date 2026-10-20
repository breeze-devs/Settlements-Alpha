package dev.breezes.settlements.infrastructure.network.features.arrival.packet;

import dev.breezes.settlements.infrastructure.minecraft.attachments.SettlementSummary;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClientBoundSettlementArrivalPacketTest {

    private static final ResourceKey<Level> NETHER =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("the_nether"));

    private static final UUID SETTLEMENT_ID = new UUID(0L, 1L);

    @Test
    void codec_roundTripsARename_withItsStatusAndBothNamesInTheirPlaces() {
        // Arrange: the two names are both strings, so a codec writing them in swapped order would still decode
        ClientBoundSettlementArrivalPacket rename = ClientBoundSettlementArrivalPacket.builder()
                .announcementType(SettlementAnnouncementType.RENAMED)
                .dimension(NETHER)
                .settlementId(SETTLEMENT_ID)
                .name("Millbrook")
                .status(new SettlementSummary.Fading(3, 1, 4))
                .previousName("Ashford")
                .build();

        // Act, Assert
        assertEquals(rename, roundTrip(rename));
    }

    @Test
    void codec_roundTripsAMoment_withoutStatusOrPreviousName() {
        // Arrange
        ClientBoundSettlementArrivalPacket entry = ClientBoundSettlementArrivalPacket.builder()
                .announcementType(SettlementAnnouncementType.ENTRY)
                .dimension(NETHER)
                .settlementId(SETTLEMENT_ID)
                .name("Ashford")
                .build();

        // Act, Assert
        assertEquals(entry, roundTrip(entry));
    }

    @Test
    void construction_rejectsAPreviousName_onAMomentOtherThanARename() {
        // Arrange
        ClientBoundSettlementArrivalPacket.ClientBoundSettlementArrivalPacketBuilder entry =
                ClientBoundSettlementArrivalPacket.builder()
                        .announcementType(SettlementAnnouncementType.ENTRY)
                        .dimension(NETHER)
                        .settlementId(SETTLEMENT_ID)
                        .name("Millbrook")
                        .previousName("Ashford");

        // Act, Assert
        assertThrows(IllegalArgumentException.class, entry::build);
    }

    @Test
    void construction_rejectsARename_withoutItsPreviousName() {
        // Arrange: the client would have no former name to show under the new one
        ClientBoundSettlementArrivalPacket.ClientBoundSettlementArrivalPacketBuilder rename =
                ClientBoundSettlementArrivalPacket.builder()
                        .announcementType(SettlementAnnouncementType.RENAMED)
                        .dimension(NETHER)
                        .settlementId(SETTLEMENT_ID)
                        .name("Millbrook");

        // Act, Assert
        assertThrows(IllegalArgumentException.class, rename::build);
    }

    private static ClientBoundSettlementArrivalPacket roundTrip(ClientBoundSettlementArrivalPacket packet) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        ClientBoundSettlementArrivalPacket.CODEC.encode(buffer, packet);
        return ClientBoundSettlementArrivalPacket.CODEC.decode(buffer);
    }

}
