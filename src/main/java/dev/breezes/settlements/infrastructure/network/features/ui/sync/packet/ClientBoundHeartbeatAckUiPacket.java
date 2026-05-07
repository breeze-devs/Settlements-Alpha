package dev.breezes.settlements.infrastructure.network.features.ui.sync.packet;

import dev.breezes.settlements.infrastructure.network.core.ClientBoundPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.UiChannel;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import javax.annotation.Nonnull;

public record ClientBoundHeartbeatAckUiPacket(@Nonnull UiChannel channel, long sessionId) implements ClientBoundPacket {

    public static final Type<ClientBoundHeartbeatAckUiPacket> ID = new Type<>(ResourceLocationUtil.mod("ui_sync_heartbeat_ack_clientbound"));
    public static final StreamCodec<FriendlyByteBuf, ClientBoundHeartbeatAckUiPacket> CODEC =
            CustomPacketPayload.codec(ClientBoundHeartbeatAckUiPacket::write, ClientBoundHeartbeatAckUiPacket::decode);

    private static ClientBoundHeartbeatAckUiPacket decode(@Nonnull FriendlyByteBuf buffer) {
        return new ClientBoundHeartbeatAckUiPacket(UiChannelCodec.read(buffer), buffer.readLong());
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
