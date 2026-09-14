package dev.breezes.settlements.infrastructure.network.features.ballista.packet;

import dev.breezes.settlements.infrastructure.network.core.ServerBoundPacket;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import javax.annotation.Nonnull;

/**
 * A player's adjustment of a ballista's aim, as the whole intent rather than a step, so that when several players
 * adjust one machine the last adjustment to arrive is where it points.
 */
public record ServerBoundBallistaAimPacket(@Nonnull BlockPos pos, float yaw, float pitch) implements ServerBoundPacket {

    public static final Type<ServerBoundBallistaAimPacket> ID =
            new Type<>(ResourceLocationUtil.mod("packet_ballista_aim_serverbound"));

    public static final StreamCodec<FriendlyByteBuf, ServerBoundBallistaAimPacket> CODEC =
            CustomPacketPayload.codec(ServerBoundBallistaAimPacket::write, ServerBoundBallistaAimPacket::decode);

    private static ServerBoundBallistaAimPacket decode(@Nonnull FriendlyByteBuf buffer) {
        return new ServerBoundBallistaAimPacket(buffer.readBlockPos(), buffer.readFloat(), buffer.readFloat());
    }

    private void write(@Nonnull FriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
        buffer.writeFloat(this.yaw);
        buffer.writeFloat(this.pitch);
    }

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

}
