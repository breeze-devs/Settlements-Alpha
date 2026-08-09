package dev.breezes.settlements.infrastructure.rendering.highlight;

import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Marks a {@link MultiBufferSource} that belongs to a supplementary pass — an entity drawn a second time in a
 * frame vanilla has already drawn it in, to feed an effect buffer.
 * <p>
 * Such a pass re-enters the entity render callbacks, so anything hanging off those callbacks that advances
 * time-based state or draws decoration unrelated to the effect must skip it. It is the same frame seen twice,
 * not a new one: work done per call rather than per frame runs double for every entity in the pass.
 */
@ClientSide
public interface SupplementaryEntityPass {
}
