package dev.breezes.settlements.infrastructure.network.features.ui.sync.packet;

import dev.breezes.settlements.infrastructure.network.core.ClientBoundPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.UiChannel;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import javax.annotation.Nonnull;

public record ClientBoundUiUnavailablePacket(@Nonnull UiChannel channel,
                                             long sessionId,
                                             @Nonnull String reasonKey) implements ClientBoundPacket {

    private static final int MAX_REASON_KEY_LENGTH = 256;

    public static final Type<ClientBoundUiUnavailablePacket> ID = new Type<>(ResourceLocationUtil.mod("ui_sync_unavailable_clientbound"));
    public static final StreamCodec<FriendlyByteBuf, ClientBoundUiUnavailablePacket> CODEC =
            CustomPacketPayload.codec(ClientBoundUiUnavailablePacket::write, ClientBoundUiUnavailablePacket::decode);

    private static ClientBoundUiUnavailablePacket decode(@Nonnull FriendlyByteBuf buffer) {
        return new ClientBoundUiUnavailablePacket(UiChannelCodec.read(buffer), buffer.readLong(), buffer.readUtf(MAX_REASON_KEY_LENGTH));
    }

    private void write(@Nonnull FriendlyByteBuf buffer) {
        UiChannelCodec.write(buffer, this.channel);
        buffer.writeLong(this.sessionId);
        buffer.writeUtf(this.reasonKey, MAX_REASON_KEY_LENGTH);
    }

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

}
