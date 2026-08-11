package dev.breezes.settlements.presentation.ui.hud;

import dev.breezes.settlements.shared.annotations.functional.ClientSide;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Contributes content to the crosshair HUD region for the current frame.
 * <p>
 * Not implemented directly — {@link HeldItemHudProvider} and {@link LookTargetHudProvider} extend this to key
 * their multibindings apart while sharing one contract, so {@link CrosshairHudRenderer} can order either
 * layer's contributors the same way.
 */
@ClientSide
public interface HudSurfaceProvider {

    /**
     * Cross-provider precedence within this surface's layer. Larger values win.
     */
    int priority();

    Optional<HudContent> contentFor(@Nonnull HudFrame frame);

}
