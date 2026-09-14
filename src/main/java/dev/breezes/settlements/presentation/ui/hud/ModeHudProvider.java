package dev.breezes.settlements.presentation.ui.hud;

import dev.breezes.settlements.shared.annotations.functional.ClientSide;

/**
 * A {@link HudSurfaceProvider} keyed on a mode the player has entered and not yet left.
 * <p>
 * Absence means the player is not in this provider's mode.
 */
@ClientSide
public interface ModeHudProvider extends HudSurfaceProvider {
}
