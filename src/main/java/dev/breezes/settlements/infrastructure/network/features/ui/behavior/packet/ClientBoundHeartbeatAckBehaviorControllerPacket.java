package dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet;

import dev.breezes.settlements.infrastructure.network.core.ClientBoundPacket;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import javax.annotation.Nonnull;

public record ClientBoundHeartbeatAckBehaviorControllerPacket(long sessionId) implements ClientBoundPacket {

    public static final Type<ClientBoundHeartbeatAckBehaviorControllerPacket> ID =
            new Type<>(ResourceLocationUtil.mod("packet_behavior_controller_heartbeat_ack_clientbound"));

    public static final StreamCodec<FriendlyByteBuf, ClientBoundHeartbeatAckBehaviorControllerPacket> CODEC =
            CustomPacketPayload.codec(ClientBoundHeartbeatAckBehaviorControllerPacket::write,
                    ClientBoundHeartbeatAckBehaviorControllerPacket::decode);

    @Nonnull
    private static ClientBoundHeartbeatAckBehaviorControllerPacket decode(@Nonnull FriendlyByteBuf buffer) {
        return new ClientBoundHeartbeatAckBehaviorControllerPacket(buffer.readLong());
    }

    private void write(@Nonnull FriendlyByteBuf buffer) {
        buffer.writeLong(this.sessionId);
    }

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

}
