package dev.breezes.settlements.infrastructure.network.features.flare.packet;

import dev.breezes.settlements.infrastructure.network.core.ClientBoundPacket;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Fireworks for the receiving client to draw.
 * <p>
 * A flare is drawn on the client manually instead of being spawned as a rocket entity because a rocket tracks to clients
 * only within 64 blocks. A flare packet reaches clients at any distance.
 *
 * @param hideWithin the horizontal distance from a flare's launch point within which the receiving client skips that
 *                   flare, in blocks; zero shows every flare
 * @param flares     at most MAX_FLARES
 */
public record ClientBoundFlarePacket(double hideWithin, @Nonnull List<Flare> flares) implements ClientBoundPacket {

    public static final int MAX_FLARES = 64;

    public static final Type<ClientBoundFlarePacket> ID = new Type<>(ResourceLocationUtil.mod("packet_flare_clientbound"));

    private static final StreamCodec<ByteBuf, List<FireworkExplosion>> EXPLOSIONS_CODEC =
            FireworkExplosion.STREAM_CODEC.apply(ByteBufCodecs.list(Fireworks.MAX_EXPLOSIONS));

    public static final StreamCodec<FriendlyByteBuf, ClientBoundFlarePacket> CODEC =
            CustomPacketPayload.codec(ClientBoundFlarePacket::write, ClientBoundFlarePacket::decode);

    public ClientBoundFlarePacket {
        Validate.isTrue(hideWithin >= 0, "hideWithin must be at least 0: %s", hideWithin);
        Validate.isTrue(flares.size() <= MAX_FLARES, "flares must number at most %s: %s", MAX_FLARES, flares.size());
        flares = List.copyOf(flares);
    }

    private static ClientBoundFlarePacket decode(@Nonnull FriendlyByteBuf buffer) {
        double hideWithin = buffer.readDouble();
        // Checked before allocating, since the count comes off the wire
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_FLARES) {
            throw new IllegalArgumentException("Invalid flare count: " + count);
        }

        List<Flare> flares = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Vec3 launch = buffer.readVec3();
            double apexY = buffer.readDouble();
            flares.add(new Flare(launch, apexY, EXPLOSIONS_CODEC.decode(buffer)));
        }
        return new ClientBoundFlarePacket(hideWithin, flares);
    }

    private void write(@Nonnull FriendlyByteBuf buffer) {
        buffer.writeDouble(this.hideWithin);
        buffer.writeVarInt(this.flares.size());
        for (Flare flare : this.flares) {
            buffer.writeVec3(flare.launch());
            buffer.writeDouble(flare.apexY());
            EXPLOSIONS_CODEC.encode(buffer, flare.explosions());
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    /**
     * One firework flare.
     *
     * @param launch     where the trail starts
     * @param apexY      the height of the burst, should be above the launch point
     * @param explosions the burst, as a firework rocket carries it; at most Fireworks.MAX_EXPLOSIONS
     */
    public record Flare(@Nonnull Vec3 launch, double apexY, @Nonnull List<FireworkExplosion> explosions) {

        public Flare {
            Validate.isTrue(apexY >= launch.y, "apexY must not be below the launch point: %s < %s", apexY, launch.y);
            Validate.isTrue(explosions.size() <= Fireworks.MAX_EXPLOSIONS, "explosions must number at most %s: %s",
                    Fireworks.MAX_EXPLOSIONS, explosions.size());
            explosions = List.copyOf(explosions);
        }

    }

}
