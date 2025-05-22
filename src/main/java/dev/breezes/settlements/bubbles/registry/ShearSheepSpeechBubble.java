package dev.breezes.settlements.bubbles.registry;

import dev.breezes.settlements.bubbles.canvas.BubbleBoundaryElement;
import dev.breezes.settlements.bubbles.canvas.DefaultSpeechBubble;
import dev.breezes.settlements.util.Ticks;

import javax.annotation.Nonnull;
import java.util.UUID;

public class ShearSheepSpeechBubble extends DefaultSpeechBubble {

    private static final BubbleShearSheepSpriteElement SPRITE_ELEMENT = BubbleShearSheepSpriteElement.builder()
            .frameDurationShears(Ticks.seconds(0.5))
            .frameDurationSheep(Ticks.seconds(0.6))
            .build();

    private static final BubbleBoundaryElement BOUNDARY_ELEMENT = BubbleBoundaryElement.builder()
            .innerElement(SPRITE_ELEMENT)
            .opacity(1.0F)
            .build();

    public ShearSheepSpeechBubble(@Nonnull UUID uuid,
                                  double visibilityBlocks,
                                  @Nonnull Ticks lifetime) {
        super(uuid, BOUNDARY_ELEMENT.copy(), visibilityBlocks, lifetime);
    }

}
