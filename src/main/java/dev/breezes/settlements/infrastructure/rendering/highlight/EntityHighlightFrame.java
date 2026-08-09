package dev.breezes.settlements.infrastructure.rendering.highlight;

import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.Builder;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Per-frame inputs a provider may need to decide what to contribute.
 * <p>
 * A provider that needs a coarser cadence than per-frame (a world scan, say) caches internally
 * against game time rather than being handed one here; that is a provider-private concern the
 * renderer must not know about.
 *
 * @param aimedAtEntity what the player's crosshair is on, or null for nothing. Resolved once here rather than
 *                      per provider, because the renderer needs the same answer to keep that entity out of
 *                      the truncation the highlight cap performs.
 */
@ClientSide
@Builder
public record EntityHighlightFrame(@Nonnull LocalPlayer localPlayer,
                                   float partialTick,
                                   @Nonnull Frustum frustum,
                                   @Nullable Entity aimedAtEntity) {
}
