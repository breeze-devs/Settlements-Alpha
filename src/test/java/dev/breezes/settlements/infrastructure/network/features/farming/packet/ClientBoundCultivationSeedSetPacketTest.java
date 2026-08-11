package dev.breezes.settlements.infrastructure.network.features.farming.packet;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

class ClientBoundCultivationSeedSetPacketTest {

    @Test
    void codec_roundTripsSeedIdsInOrder() {
        List<ResourceLocation> input = List.of(
                ResourceLocation.withDefaultNamespace("wheat_seeds"),
                ResourceLocation.withDefaultNamespace("carrot"),
                ResourceLocation.fromNamespaceAndPath("settlements", "custom_seed"));

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        ClientBoundCultivationSeedSetPacket.CODEC.encode(buffer, new ClientBoundCultivationSeedSetPacket(input));

        Assertions.assertEquals(input, ClientBoundCultivationSeedSetPacket.CODEC.decode(buffer).seedItemIds());
    }

    @Test
    void codec_roundTripsEmptyCatalog() {
        // An empty catalog is legal on this wire, unlike a bubble's segment list: a datapack that
        // defines no crops must still be able to tell the client so, or the client would keep
        // answering "is this a seed" from a previous datapack's set.
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        ClientBoundCultivationSeedSetPacket.CODEC.encode(buffer, new ClientBoundCultivationSeedSetPacket(List.of()));

        Assertions.assertTrue(ClientBoundCultivationSeedSetPacket.CODEC.decode(buffer).seedItemIds().isEmpty());
    }

    @Test
    void codec_roundTripsAtTheCeiling() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        ClientBoundCultivationSeedSetPacket.CODEC.encode(buffer, new ClientBoundCultivationSeedSetPacket(seedIds(
                ClientBoundCultivationSeedSetPacket.MAX_ENTRIES)));

        Assertions.assertEquals(ClientBoundCultivationSeedSetPacket.MAX_ENTRIES,
                ClientBoundCultivationSeedSetPacket.CODEC.decode(buffer).seedItemIds().size());
    }

    @Test
    void encode_rejectsCatalogAboveTheCeiling() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        ClientBoundCultivationSeedSetPacket packet = new ClientBoundCultivationSeedSetPacket(seedIds(
                ClientBoundCultivationSeedSetPacket.MAX_ENTRIES + 1));

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> ClientBoundCultivationSeedSetPacket.CODEC.encode(buffer, packet));
    }

    @Test
    void decode_rejectsLengthPrefixAboveTheCeiling() {
        // Hand-written prefix rather than an encoded packet: encode refuses to produce this, and the
        // ceiling exists precisely for a buffer this side did not write.
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(ClientBoundCultivationSeedSetPacket.MAX_ENTRIES + 1);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> ClientBoundCultivationSeedSetPacket.CODEC.decode(buffer));
    }

    @Test
    void decode_rejectsNegativeLengthPrefix() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(-1);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> ClientBoundCultivationSeedSetPacket.CODEC.decode(buffer));
    }

    @Test
    void construction_copiesTheSuppliedList() {
        List<ResourceLocation> mutable = new ArrayList<>(
                List.of(ResourceLocation.withDefaultNamespace("wheat_seeds")));
        ClientBoundCultivationSeedSetPacket packet = new ClientBoundCultivationSeedSetPacket(mutable);

        mutable.clear();

        Assertions.assertEquals(1, packet.seedItemIds().size());
    }

    private static List<ResourceLocation> seedIds(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> ResourceLocation.fromNamespaceAndPath("settlements", "seed_" + index))
                .toList();
    }

}
