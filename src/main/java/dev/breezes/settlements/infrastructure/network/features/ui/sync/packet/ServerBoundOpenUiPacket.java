package dev.breezes.settlements.infrastructure.network.features.ui.sync.packet;

import dev.breezes.settlements.infrastructure.network.core.ServerBoundPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.UiChannel;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import javax.annotation.Nonnull;

public record ServerBoundOpenUiPacket(@Nonnull UiChannel channel, int villagerEntityId) implements ServerBoundPacket {

    public static final Type<ServerBoundOpenUiPacket> ID = new Type<>(ResourceLocationUtil.mod("ui_sync_open_serverbound"));
    public static final StreamCodec<FriendlyByteBuf, ServerBoundOpenUiPacket> CODEC =
            CustomPacketPayload.codec(ServerBoundOpenUiPacket::write, ServerBoundOpenUiPacket::decode);

    private static ServerBoundOpenUiPacket decode(@Nonnull FriendlyByteBuf buffer) {
        return new ServerBoundOpenUiPacket(UiChannelCodec.read(buffer), buffer.readInt());
    }

    private void write(@Nonnull FriendlyByteBuf buffer) {
        UiChannelCodec.write(buffer, this.channel);
        buffer.writeInt(this.villagerEntityId);
    }

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

}
