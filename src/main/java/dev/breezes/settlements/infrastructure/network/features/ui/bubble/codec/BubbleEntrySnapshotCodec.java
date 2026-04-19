package dev.breezes.settlements.infrastructure.network.features.ui.bubble.codec;

import dev.breezes.settlements.application.ui.bubble.BubbleChannel;
import dev.breezes.settlements.application.ui.bubble.BubbleEntrySnapshot;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public final class BubbleEntrySnapshotCodec {

    private static final int MAX_OWNER_KEY_LENGTH = 128;
    private static final int MAX_TEXT_LENGTH = 256;

    public static BubbleEntrySnapshot read(@Nonnull FriendlyByteBuf buffer) {
        return BubbleEntrySnapshot.builder()
                .bubbleId(buffer.readUUID())
                .channel(buffer.readEnum(BubbleChannel.class))
                .ownerKey(readOptionalString(buffer, MAX_OWNER_KEY_LENGTH).orElse(null))
                .priority(buffer.readVarInt())
                .expireGameTime(buffer.readLong())
                .createdGameTime(buffer.readLong())
                .sequenceNumber(buffer.readLong())
                .contentVersion(buffer.readVarInt())
                .sourceType(buffer.readUtf(MAX_TEXT_LENGTH))
                .segments(BubbleSegmentCodec.readList(buffer))
                .build();
    }

    public static void write(@Nonnull FriendlyByteBuf buffer, @Nonnull BubbleEntrySnapshot snapshot) {
        buffer.writeUUID(snapshot.bubbleId());
        buffer.writeEnum(snapshot.channel());
        writeOptionalString(buffer, snapshot.ownerKey(), MAX_OWNER_KEY_LENGTH);
        buffer.writeVarInt(snapshot.priority());
        buffer.writeLong(snapshot.expireGameTime());
        buffer.writeLong(snapshot.createdGameTime());
        buffer.writeLong(snapshot.sequenceNumber());
        buffer.writeVarInt(snapshot.contentVersion());
        buffer.writeUtf(snapshot.sourceType(), MAX_TEXT_LENGTH);
        BubbleSegmentCodec.writeList(buffer, snapshot.segments());
    }

    private static Optional<String> readOptionalString(@Nonnull FriendlyByteBuf buffer, int maxLength) {
        return buffer.readBoolean() ? Optional.of(buffer.readUtf(maxLength)) : Optional.empty();
    }

    private static void writeOptionalString(@Nonnull FriendlyByteBuf buffer, @Nullable String value, int maxLength) {
        boolean present = value != null;
        buffer.writeBoolean(present);
        if (present) {
            buffer.writeUtf(value, maxLength);
        }
    }

}
