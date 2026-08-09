package dev.breezes.settlements.infrastructure.rendering.highlight;

import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nonnull;

/**
 * Accepts one entity's outline color for the current frame.
 */
@ClientSide
public interface EntityHighlightSink {

    /**
     * @param argbColor packed 0xAARRGGBB
     */
    void highlight(@Nonnull Entity entity, int argbColor);

}
