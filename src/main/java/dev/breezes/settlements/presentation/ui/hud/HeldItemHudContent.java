package dev.breezes.settlements.presentation.ui.hud;

import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.Builder;
import lombok.Singular;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * What a {@link HeldItemHudProvider} wants drawn for the currently held item — e.g. text and an accent color.
 */
@ClientSide
@Builder
public record HeldItemHudContent(@Nonnull Component headline,
                                 @Nonnull @Singular("detail") List<Component> details,
                                 int accentColor) {
}
