package dev.breezes.settlements.presentation.ui.hud;

import dev.breezes.settlements.shared.annotations.functional.ClientSide;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Shared contract for crosshair HUD content.
 * Implement {@link ModeHudProvider}, {@link LookTargetHudProvider} or {@link HeldItemHudProvider} to select a layer.
 */
@ClientSide
public interface HudSurfaceProvider {

    /**
     * Priority within this provider's layer. Higher values take precedence.
     */
    int priority();

    /**
     * Returns content for this frame, or empty when this provider has nothing to show.
     */
    Optional<HudContent> contentFor(@Nonnull HudFrame frame);

}
