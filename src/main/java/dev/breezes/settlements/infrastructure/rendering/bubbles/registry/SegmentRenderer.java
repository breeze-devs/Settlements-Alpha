package dev.breezes.settlements.infrastructure.rendering.bubbles.registry;

import dev.breezes.settlements.application.ui.bubble.BubbleSegment;
import dev.breezes.settlements.infrastructure.rendering.bubbles.canvas.AnimatedSpriteElement;
import dev.breezes.settlements.infrastructure.rendering.bubbles.canvas.BubbleHorizontalCompositeElement;
import dev.breezes.settlements.infrastructure.rendering.bubbles.canvas.BubbleInnerElement;
import dev.breezes.settlements.infrastructure.rendering.bubbles.canvas.BubbleItemStackElement;
import dev.breezes.settlements.infrastructure.rendering.bubbles.canvas.BubbleTextElement;
import dev.breezes.settlements.infrastructure.rendering.bubbles.texture.SpriteCatalog;
import lombok.CustomLog;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nonnull;
import java.util.List;

@CustomLog
public final class SegmentRenderer {

    private static final int DEFAULT_TEXT_MAX_WIDTH = 140;
    private static final int ITEM_COUNT_TEXT_MAX_WIDTH = 60;

    public static BubbleInnerElement toElement(@Nonnull BubbleSegment segment) {
        return switch (segment) {
            case BubbleSegment.Item item -> buildItem(item);
            case BubbleSegment.Text text -> buildText(text);
            case BubbleSegment.Translatable translatable -> buildTranslatable(translatable);
            case BubbleSegment.Sprite sprite -> buildSprite(sprite);
        };
    }

    private static BubbleInnerElement buildItem(BubbleSegment.Item item) {
        Item resolvedItem = BuiltInRegistries.ITEM.getOptional(item.itemId())
                .filter(candidate -> candidate != Items.AIR)
                .orElseGet(() -> {
                    log.error("Unknown item {}, using placeholder item", item.itemId());
                    return Items.BARRIER;
                });

        BubbleItemStackElement icon = BubbleItemStackElement.builder()
                .stack(new ItemStack(resolvedItem))
                .build();

        if (item.count() == 0) {
            return icon;
        }

        // Count formatting lives in the renderer so server-side behavior code stays semantic.
        return BubbleHorizontalCompositeElement.builder()
                .children(List.of(
                        icon,
                        buildText(BubbleSegment.Text.builder()
                                .literal("×" + item.count())
                                .color(ChatFormatting.BLACK)
                                .bold(true)
                                .scale(0.7F)
                                .build(), ITEM_COUNT_TEXT_MAX_WIDTH)
                ))
                .gap(2)
                .build();
    }

    private static BubbleInnerElement buildText(BubbleSegment.Text text) {
        return buildText(text, DEFAULT_TEXT_MAX_WIDTH);
    }

    private static BubbleInnerElement buildText(BubbleSegment.Text text, int maxWidth) {
        MutableComponent component = Component.literal(text.literal()).withStyle(text.color());
        if (text.bold()) {
            component = component.withStyle(ChatFormatting.BOLD);
        }
        return BubbleTextElement.builder()
                .component(component)
                .scale(text.scale())
                .maxWidth(maxWidth)
                .build();
    }

    private static BubbleInnerElement buildTranslatable(BubbleSegment.Translatable translatable) {
        MutableComponent component = Component.translatable(translatable.key(), translatable.args().toArray()).withStyle(translatable.color());
        if (translatable.bold()) {
            component = component.withStyle(ChatFormatting.BOLD);
        }
        return BubbleTextElement.builder()
                .component(component)
                .scale(translatable.scale())
                .maxWidth(DEFAULT_TEXT_MAX_WIDTH)
                .build();
    }

    private static BubbleInnerElement buildSprite(BubbleSegment.Sprite sprite) {
        return AnimatedSpriteElement.builder()
                .texture(SpriteCatalog.resolve(sprite.sprite()))
                .frameDuration(sprite.frameDuration())
                .build();
    }

}
