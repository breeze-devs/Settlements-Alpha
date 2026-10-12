package dev.breezes.settlements.infrastructure.network.features.flare.packet;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

class ClientBoundFlarePacketTest {

    private static final FireworkExplosion BURST =
            new FireworkExplosion(FireworkExplosion.Shape.SMALL_BALL, IntList.of(0x112233), IntList.of(), false, false);

    @Test
    void codec_roundTripsEveryFlareInOrder() {
        ClientBoundFlarePacket input = new ClientBoundFlarePacket(48, List.of(
                new ClientBoundFlarePacket.Flare(new Vec3(1.5, 64, -2.25), 80, List.of(BURST)),
                new ClientBoundFlarePacket.Flare(new Vec3(-30, 70, 12), 86.5, List.of(
                        new FireworkExplosion(FireworkExplosion.Shape.LARGE_BALL, IntList.of(0xAA0000), IntList.of(0x00BB00), true, true),
                        BURST)),
                new ClientBoundFlarePacket.Flare(new Vec3(0, -10, 0), -10, List.of())));

        Assertions.assertEquals(input, roundTrip(input));
    }

    @Test
    void codec_roundTripsAtTheCeiling() {
        ClientBoundFlarePacket input = new ClientBoundFlarePacket(0, flares(ClientBoundFlarePacket.MAX_FLARES));

        Assertions.assertEquals(ClientBoundFlarePacket.MAX_FLARES, roundTrip(input).flares().size());
    }

    @Test
    void decode_rejectsCountAboveTheCeiling() {
        // Hand-written prefix rather than an encoded packet: construction refuses to produce this, and the ceiling exists
        // for a buffer this side did not write
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeDouble(0);
        buffer.writeVarInt(ClientBoundFlarePacket.MAX_FLARES + 1);

        Assertions.assertThrows(IllegalArgumentException.class, () -> ClientBoundFlarePacket.CODEC.decode(buffer));
    }

    @Test
    void decode_rejectsNegativeCount() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeDouble(0);
        buffer.writeVarInt(-1);

        Assertions.assertThrows(IllegalArgumentException.class, () -> ClientBoundFlarePacket.CODEC.decode(buffer));
    }

    @Test
    void construction_rejectsFlaresAboveTheCeiling() {
        List<ClientBoundFlarePacket.Flare> flares = flares(ClientBoundFlarePacket.MAX_FLARES + 1);

        Assertions.assertThrows(IllegalArgumentException.class, () -> new ClientBoundFlarePacket(0, flares));
    }

    @Test
    void construction_rejectsNegativeHideWithin() {
        // A negative radius squares to a positive one, so it would hide flares instead of showing every one
        List<ClientBoundFlarePacket.Flare> flares = flares(1);

        Assertions.assertThrows(IllegalArgumentException.class, () -> new ClientBoundFlarePacket(-1, flares));
    }

    @Test
    void flare_acceptsApexLevelWithTheLaunchPoint() {
        Assertions.assertDoesNotThrow(() -> new ClientBoundFlarePacket.Flare(new Vec3(0, 64, 0), 64, List.of(BURST)));
    }

    @Test
    void flare_rejectsApexBelowTheLaunchPoint() {
        Vec3 launch = new Vec3(0, 64, 0);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ClientBoundFlarePacket.Flare(launch, 63.5, List.of(BURST)));
    }

    @Test
    void flare_rejectsExplosionsAboveTheFireworkCeiling() {
        // Encoding would refuse them anyway, but on the network thread, where a dedicated server drops the connection
        List<FireworkExplosion> explosions = Collections.nCopies(Fireworks.MAX_EXPLOSIONS + 1, BURST);
        Vec3 launch = new Vec3(0, 64, 0);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ClientBoundFlarePacket.Flare(launch, 80, explosions));
    }

    private static ClientBoundFlarePacket roundTrip(ClientBoundFlarePacket packet) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        ClientBoundFlarePacket.CODEC.encode(buffer, packet);
        return ClientBoundFlarePacket.CODEC.decode(buffer);
    }

    private static List<ClientBoundFlarePacket.Flare> flares(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> new ClientBoundFlarePacket.Flare(new Vec3(index, 64, 0), 80, List.of(BURST)))
                .toList();
    }

}
