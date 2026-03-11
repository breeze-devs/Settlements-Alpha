package dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet;

import dev.breezes.settlements.infrastructure.network.core.ServerBoundPacket;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import javax.annotation.Nonnull;

public record ServerBoundHeartbeatBehaviorControllerPacket(long sessionId) implements ServerBoundPacket {

    public static final Type<ServerBoundHeartbeatBehaviorControllerPacket> ID =
            new Type<>(ResourceLocationUtil.mod("packet_behavior_controller_heartbeat_serverbound"));

    public static final StreamCodec<FriendlyByteBuf, ServerBoundHeartbeatBehaviorControllerPacket> CODEC =
            CustomPacketPayload.codec(ServerBoundHeartbeatBehaviorControllerPacket::write, ServerBoundHeartbeatBehaviorControllerPacket::decode);

    @Nonnull
    private static ServerBoundHeartbeatBehaviorControllerPacket decode(@Nonnull FriendlyByteBuf buffer) {
        return new ServerBoundHeartbeatBehaviorControllerPacket(buffer.readLong());
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
