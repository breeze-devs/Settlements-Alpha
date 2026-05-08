package dev.breezes.settlements.infrastructure.rendering.bubbles.registry;

import dev.breezes.settlements.application.ui.bubble.BubbleSegment;
import dev.breezes.settlements.application.ui.bubble.SpriteRef;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.rendering.bubbles.BubbleBoundingBox;
import dev.breezes.settlements.infrastructure.rendering.bubbles.canvas.AnimatedSpriteElement;
import dev.breezes.settlements.infrastructure.rendering.bubbles.canvas.BubbleInnerElement;
import dev.breezes.settlements.infrastructure.rendering.bubbles.canvas.BubbleTextElement;
import net.minecraft.ChatFormatting;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SegmentRendererTest {

    @Test
    void text_returnsTextElement() {
        BubbleInnerElement element = SegmentRenderer.toElement(BubbleSegment.Text.builder()
                .literal("✔")
                .color(ChatFormatting.DARK_GREEN)
                .bold(true)
                .scale(0.9F)
                .build());

        Assertions.assertInstanceOf(BubbleTextElement.class, element);
    }

    @Test
    void sprite_returnsAnimatedSpriteElement() {
        BubbleInnerElement element = SegmentRenderer.toElement(BubbleSegment.Sprite.builder()
                .sprite(SpriteRef.SHEARS)
                .frameDuration(ClockTicks.seconds(0.5))
                .build());

        Assertions.assertInstanceOf(AnimatedSpriteElement.class, element);
        assertNonDegenerate(element.getBoundingBox());
    }

    private static void assertNonDegenerate(BubbleBoundingBox boundingBox) {
        Assertions.assertTrue(boundingBox.getWidth() > 0);
        Assertions.assertTrue(boundingBox.getHeight() > 0);
    }

}
