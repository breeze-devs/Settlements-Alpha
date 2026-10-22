package dev.breezes.settlements.presentation.ui.hud;

import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.Builder;
import lombok.Singular;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * What a {@link HudSurfaceProvider} wants drawn in the crosshair HUD region — a headline, optional detail
 * lines, and an accent color.
 */
@ClientSide
@Builder
public record HudContent(Component headline,
                         @Singular("detail") List<Component> details,
                         int accentColor) {
}
