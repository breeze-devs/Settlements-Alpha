package dev.breezes.settlements.infrastructure.network.features.ui.sync.packet;

import dev.breezes.settlements.infrastructure.network.features.ui.sync.UiChannel;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nonnull;

final class UiChannelCodec {

    private UiChannelCodec() {
    }

    static UiChannel read(@Nonnull FriendlyByteBuf buffer) {
        int ordinal = buffer.readVarInt();
        UiChannel[] values = UiChannel.values();
        if (ordinal < 0 || ordinal >= values.length) {
            throw new IllegalArgumentException("Unknown UiChannel ordinal: " + ordinal);
        }
        return values[ordinal];
    }

    static void write(@Nonnull FriendlyByteBuf buffer, @Nonnull UiChannel channel) {
        buffer.writeVarInt(channel.ordinal());
    }

}
