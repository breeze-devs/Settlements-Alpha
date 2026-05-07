package dev.breezes.settlements.infrastructure.network.features.ui.sync.packet;

import dev.breezes.settlements.infrastructure.network.core.ClientBoundPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.UiChannel;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import javax.annotation.Nonnull;

public record ClientBoundOpenUiPacket(@Nonnull UiChannel channel, long sessionId, int villagerEntityId) implements ClientBoundPacket {

    public static final Type<ClientBoundOpenUiPacket> ID = new Type<>(ResourceLocationUtil.mod("ui_sync_open_clientbound"));
    public static final StreamCodec<FriendlyByteBuf, ClientBoundOpenUiPacket> CODEC =
            CustomPacketPayload.codec(ClientBoundOpenUiPacket::write, ClientBoundOpenUiPacket::decode);

    private static ClientBoundOpenUiPacket decode(@Nonnull FriendlyByteBuf buffer) {
        return new ClientBoundOpenUiPacket(UiChannelCodec.read(buffer), buffer.readLong(), buffer.readInt());
    }

    private void write(@Nonnull FriendlyByteBuf buffer) {
        UiChannelCodec.write(buffer, this.channel);
        buffer.writeLong(this.sessionId);
        buffer.writeInt(this.villagerEntityId);
    }

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

}
