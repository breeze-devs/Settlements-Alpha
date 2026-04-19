package dev.breezes.settlements.application.ui.bubble;

import dev.breezes.settlements.domain.time.Ticks;
import lombok.Builder;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

/**
 * Closed vocabulary describing bubble content independent from concrete rendering templates.
 * <p>
 * Keeping the wire model at this level lets application code express meaning compositionally
 * while the client remains free to decide how those primitives are drawn.
 */
public sealed interface BubbleSegment permits BubbleSegment.Item, BubbleSegment.Text, BubbleSegment.Sprite {

    @Builder
    record Item(@Nonnull ResourceLocation itemId, int count) implements BubbleSegment {

        public Item {
            if (count < 0) {
                throw new IllegalArgumentException("count must be >= 0");
            }
        }

        /**
         * Convenience factory for icon-only item segments.
         */
        public static Item iconOnly(@Nonnull ResourceLocation itemId) {
            return Item.builder()
                    .itemId(itemId)
                    .count(0)
                    .build();
        }

    }

    @Builder
    record Text(@Nonnull String literal,
                @Nonnull ChatFormatting color,
                boolean bold,
                float scale) implements BubbleSegment {

        public Text {
            if (literal.isBlank()) {
                throw new IllegalArgumentException("literal must not be blank");
            }
            if (scale <= 0.0F) {
                throw new IllegalArgumentException("scale must be > 0");
            }
        }

    }

    @Builder
    record Sprite(@Nonnull SpriteRef sprite,
                  @Nonnull Ticks frameDuration) implements BubbleSegment {
    }

}
