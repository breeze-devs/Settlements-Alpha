package dev.breezes.settlements.infrastructure.rendering.bubbles.texture;

import dev.breezes.settlements.application.ui.bubble.SpriteRef;

import javax.annotation.Nonnull;
import java.util.Map;

public final class SpriteCatalog {

    private static final Map<SpriteRef, AnimatedFrameTexture> TEXTURES = Map.of(
            SpriteRef.SHEARS, AnimatedFrameTexture.builder()
                    .id("bubble_shears")
                    .texturePath("textures/bubble/anim_bubble_shears.png")
                    .width(64)
                    .height(32)
                    .frameWidth(32)
                    .frameCount(2)
                    .build(),
            SpriteRef.SHEEP, AnimatedFrameTexture.builder()
                    .id("bubble_sheep")
                    .texturePath("textures/bubble/anim_bubble_sheep.png")
                    .width(64)
                    .height(32)
                    .frameWidth(32)
                    .frameCount(2)
                    .build()
    );

    private SpriteCatalog() {
    }

    public static AnimatedFrameTexture resolve(@Nonnull SpriteRef ref) {
        AnimatedFrameTexture texture = TEXTURES.get(ref);
        if (texture == null) {
            throw new IllegalStateException("No texture registered for sprite: " + ref);
        }
        return texture;
    }

}
