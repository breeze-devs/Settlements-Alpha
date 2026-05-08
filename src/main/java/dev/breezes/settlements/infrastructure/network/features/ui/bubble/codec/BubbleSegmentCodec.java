package dev.breezes.settlements.infrastructure.network.features.ui.bubble.codec;

import dev.breezes.settlements.application.ui.bubble.BubbleSegment;
import dev.breezes.settlements.application.ui.bubble.SpriteRef;
import dev.breezes.settlements.domain.time.ClockTicks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public final class BubbleSegmentCodec {

    private static final byte TYPE_ITEM = 0x01;
    private static final byte TYPE_TEXT = 0x02;
    private static final byte TYPE_SPRITE = 0x03;

    public static final int MAX_SEGMENTS = 8;
    public static final int MAX_TEXT_LENGTH = 128;
    private static final int MAX_FORMATTING_CODE_LENGTH = 1;
    private static final int MAX_SPRITE_NAME_LENGTH = 64;

    public static BubbleSegment read(@Nonnull FriendlyByteBuf buffer) {
        return switch (buffer.readByte()) {
            case TYPE_ITEM -> BubbleSegment.Item.builder()
                    .itemId(buffer.readResourceLocation())
                    .count(buffer.readVarInt())
                    .build();
            case TYPE_TEXT -> BubbleSegment.Text.builder()
                    .literal(buffer.readUtf(MAX_TEXT_LENGTH))
                    .color(readFormatting(buffer.readUtf(MAX_FORMATTING_CODE_LENGTH)))
                    .bold(buffer.readBoolean())
                    .scale(buffer.readFloat())
                    .build();
            case TYPE_SPRITE -> BubbleSegment.Sprite.builder()
                    .sprite(SpriteRef.valueOf(buffer.readUtf(MAX_SPRITE_NAME_LENGTH)))
                    .frameDuration(ClockTicks.of(buffer.readVarLong()))
                    .build();
            default -> throw new IllegalArgumentException("Unknown bubble segment type");
        };
    }

    public static void write(@Nonnull FriendlyByteBuf buffer, @Nonnull BubbleSegment segment) {
        switch (segment) {
            case BubbleSegment.Item item -> {
                buffer.writeByte(TYPE_ITEM);
                buffer.writeResourceLocation(item.itemId());
                buffer.writeVarInt(item.count());
            }
            case BubbleSegment.Text text -> {
                buffer.writeByte(TYPE_TEXT);
                buffer.writeUtf(text.literal(), MAX_TEXT_LENGTH);
                buffer.writeUtf(String.valueOf(text.color().getChar()), MAX_FORMATTING_CODE_LENGTH);
                buffer.writeBoolean(text.bold());
                buffer.writeFloat(text.scale());
            }
            case BubbleSegment.Sprite sprite -> {
                buffer.writeByte(TYPE_SPRITE);
                buffer.writeUtf(sprite.sprite().name(), MAX_SPRITE_NAME_LENGTH);
                buffer.writeVarLong(sprite.frameDuration().getTicks());
            }
        }
    }

    public static List<BubbleSegment> readList(@Nonnull FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 1 || size > MAX_SEGMENTS) {
            throw new IllegalArgumentException("Invalid bubble segment count: " + size);
        }

        List<BubbleSegment> segments = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            segments.add(read(buffer));
        }
        return segments;
    }

    public static void writeList(@Nonnull FriendlyByteBuf buffer, @Nonnull List<BubbleSegment> segments) {
        if (segments.isEmpty() || segments.size() > MAX_SEGMENTS) {
            throw new IllegalArgumentException("Invalid bubble segment count: " + segments.size());
        }

        buffer.writeVarInt(segments.size());
        for (BubbleSegment segment : segments) {
            write(buffer, segment);
        }
    }

    private static ChatFormatting readFormatting(@Nonnull String formattingCode) {
        if (formattingCode.length() != 1) {
            throw new IllegalArgumentException("Invalid ChatFormatting code: " + formattingCode);
        }

        char code = formattingCode.charAt(0);
        for (ChatFormatting formatting : ChatFormatting.values()) {
            if (formatting.getChar() == code) {
                return formatting;
            }
        }

        throw new IllegalArgumentException("Invalid ChatFormatting code: " + formattingCode);
    }

}
