package dev.breezes.settlements.infrastructure.network.features.ui.bubble.codec;

import dev.breezes.settlements.application.ui.bubble.BubbleChannel;
import dev.breezes.settlements.application.ui.bubble.BubbleEntrySnapshot;
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
import java.util.UUID;

class BubbleEntrySnapshotCodecTest {

    @Test
    void bubbleEntrySnapshot_roundtrip_preservesFields() {
        BubbleEntrySnapshot input = BubbleEntrySnapshot.builder()
                .bubbleId(UUID.randomUUID())
                .channel(BubbleChannel.BEHAVIOR)
                .ownerKey("test:codec_roundtrip")
                .priority(50)
                .expireGameTime(400)
                .createdGameTime(200)
                .sequenceNumber(3)
                .contentVersion(2)
                .sourceType("behavior")
                .segments(List.of(
                        BubbleSegment.Item.builder()
                                .itemId(ResourceLocation.withDefaultNamespace("bread"))
                                .count(2)
                                .build(),
                        BubbleSegment.Text.builder()
                                .literal("✔")
                                .color(ChatFormatting.DARK_GREEN)
                                .bold(true)
                                .scale(0.9F)
                                .build(),
                        BubbleSegment.Sprite.builder()
                                .sprite(SpriteRef.SHEARS)
                                .frameDuration(ClockTicks.seconds(1))
                                .build()))
                .build();

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        BubbleEntrySnapshotCodec.write(buffer, input);

        BubbleEntrySnapshot decoded = BubbleEntrySnapshotCodec.read(buffer);
        Assertions.assertEquals(input, decoded);
    }

    @Test
    void bubbleEntrySnapshot_roundtrip_preservesNullOwnerKey() {
        BubbleEntrySnapshot input = BubbleEntrySnapshot.builder()
                .bubbleId(UUID.randomUUID())
                .channel(BubbleChannel.CHAT)
                .ownerKey(null)
                .priority(5)
                .expireGameTime(90)
                .createdGameTime(10)
                .sequenceNumber(1)
                .contentVersion(0)
                .sourceType("sensor")
                .segments(List.of(BubbleSegment.Text.builder()
                        .literal("hello")
                        .color(ChatFormatting.BLACK)
                        .bold(false)
                        .scale(1.0F)
                        .build()))
                .build();

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        BubbleEntrySnapshotCodec.write(buffer, input);

        BubbleEntrySnapshot decoded = BubbleEntrySnapshotCodec.read(buffer);
        Assertions.assertEquals(input, decoded);
    }

    @Test
    void bubbleEntrySnapshot_roundtrip_preservesTradeBubblePayload() {
        BubbleEntrySnapshot input = BubbleEntrySnapshot.builder()
                .bubbleId(UUID.randomUUID())
                .channel(BubbleChannel.BEHAVIOR)
                .ownerKey("trade-session-1")
                .priority(10)
                .expireGameTime(140)
                .createdGameTime(100)
                .sequenceNumber(2)
                .contentVersion(0)
                .sourceType("trade")
                .segments(List.of(
                        BubbleSegment.Item.builder()
                                .itemId(ResourceLocation.withDefaultNamespace("wheat"))
                                .count(4)
                                .build(),
                        BubbleSegment.Item.builder()
                                .itemId(ResourceLocation.withDefaultNamespace("emerald"))
                                .count(3)
                                .build(),
                        BubbleSegment.Text.builder()
                                .literal("✔")
                                .color(ChatFormatting.DARK_GREEN)
                                .bold(true)
                                .scale(0.9F)
                                .build()))
                .build();

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        BubbleEntrySnapshotCodec.write(buffer, input);

        BubbleEntrySnapshot decoded = BubbleEntrySnapshotCodec.read(buffer);
        Assertions.assertEquals(input, decoded);
    }

}
