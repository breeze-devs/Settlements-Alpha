package dev.breezes.settlements.presentation.ui.hud;

import dev.breezes.settlements.shared.annotations.functional.ClientSide;

/**
 * A {@link HudSurfaceProvider} keyed on the player's held item.
 * <p>
 * Absence means this provider has nothing to say — not every provider recognizes every item, and the region
 * renders nothing at all when every contributing surface returns empty.
 */
@ClientSide
public interface HeldItemHudProvider extends HudSurfaceProvider {
}
