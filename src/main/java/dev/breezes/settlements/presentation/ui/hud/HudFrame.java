package dev.breezes.settlements.presentation.ui.hud;

import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.Builder;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Per-frame inputs a {@link HudSurfaceProvider} may need, resolved once by the region's owner rather than by
 * each provider — see {@link CrosshairHudRenderer}. This is what lets a provider answer without touching Minecraft.
 * <p>
 * A provider that needs a coarser cadence than per-frame caches internally against
 * {@link dev.breezes.settlements.shared.util.ClientMonotonicClock} rather than being handed one here; that is
 * a provider-private concern the owner must not know about.
 *
 * @param blockHit      the crosshair's block ray trace, or null on a miss. This trace does not see fluids.
 * @param aimedAtEntity the entity under the crosshair, or null for nothing, read the same way vanilla's own
 *                      right-click dispatch picks one.
 */
@ClientSide
@Builder
public record HudFrame(@Nonnull LocalPlayer localPlayer,
                       @Nonnull ItemStack mainHandStack,
                       @Nullable BlockHitResult blockHit,
                       @Nullable Entity aimedAtEntity,
                       float partialTick) {
}
