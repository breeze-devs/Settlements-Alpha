package dev.breezes.settlements.infrastructure.network.features.ui.sync.packet;

import dev.breezes.settlements.infrastructure.network.core.ServerBoundPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.UiChannel;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import javax.annotation.Nonnull;

public record ServerBoundHeartbeatUiPacket(@Nonnull UiChannel channel, long sessionId) implements ServerBoundPacket {

    public static final Type<ServerBoundHeartbeatUiPacket> ID = new Type<>(ResourceLocationUtil.mod("ui_sync_heartbeat_serverbound"));
    public static final StreamCodec<FriendlyByteBuf, ServerBoundHeartbeatUiPacket> CODEC =
            CustomPacketPayload.codec(ServerBoundHeartbeatUiPacket::write, ServerBoundHeartbeatUiPacket::decode);

    private static ServerBoundHeartbeatUiPacket decode(@Nonnull FriendlyByteBuf buffer) {
        return new ServerBoundHeartbeatUiPacket(UiChannelCodec.read(buffer), buffer.readLong());
    }

    private void write(@Nonnull FriendlyByteBuf buffer) {
        UiChannelCodec.write(buffer, this.channel);
        buffer.writeLong(this.sessionId);
    }

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

}
