package dev.breezes.settlements.infrastructure.network.features.ui.bubble.codec;

import dev.breezes.settlements.application.ui.bubble.BubbleSegment;
import dev.breezes.settlements.application.ui.bubble.SpriteRef;
import dev.breezes.settlements.domain.time.ClockTicks;
import io.netty.buffer.Unpooled;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

class BubbleSegmentCodecTest {

    @Test
    void roundtrip_preservesEachSegmentVariant() {
        List<BubbleSegment> input = List.of(
                BubbleSegment.Item.builder()
                        .itemId(ResourceLocation.withDefaultNamespace("wheat"))
                        .count(4)
                        .build(),
                BubbleSegment.Text.builder()
                        .literal("×4")
                        .color(ChatFormatting.BLACK)
                        .bold(true)
                        .scale(0.7F)
                        .build(),
                BubbleSegment.Translatable.builder()
                        .key("dialogue.settlements.generic.idle.1")
                        .args(List.of("arg"))
                        .color(ChatFormatting.BLACK)
                        .bold(false)
                        .scale(0.85F)
                        .build(),
                BubbleSegment.Sprite.builder()
                        .sprite(SpriteRef.SHEARS)
                        .frameDuration(ClockTicks.seconds(0.5))
                        .build());

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        BubbleSegmentCodec.writeList(buffer, input);

        Assertions.assertEquals(input, BubbleSegmentCodec.readList(buffer));
    }

    @Test
    void writeList_rejectsEmptyList() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        Assertions.assertThrows(IllegalArgumentException.class, () -> BubbleSegmentCodec.writeList(buffer, List.of()));
    }

    @Test
    void writeList_rejectsTooManySegments() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        List<BubbleSegment> segments = IntStream.range(0, 9)
                .<BubbleSegment>mapToObj(index -> BubbleSegment.Text.builder()
                        .literal("s" + index)
                        .color(ChatFormatting.BLACK)
                        .bold(false)
                        .scale(1.0F)
                        .build())
                .toList();

        Assertions.assertThrows(IllegalArgumentException.class, () -> BubbleSegmentCodec.writeList(buffer, segments));
    }

    @Test
    void textSegment_rejectsBlankLiteral() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> BubbleSegment.Text.builder()
                .literal(" ")
                .color(ChatFormatting.BLACK)
                .bold(false)
                .scale(1.0F)
                .build());
    }

    @Test
    void readList_rejectsUnknownFormattingCode() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(1);
        buffer.writeByte(0x02);
        buffer.writeUtf("bad", BubbleSegmentCodec.MAX_TEXT_LENGTH);
        buffer.writeUtf("z", 1);
        buffer.writeBoolean(false);
        buffer.writeFloat(1.0F);

        Assertions.assertThrows(IllegalArgumentException.class, () -> BubbleSegmentCodec.readList(buffer));
    }

}
