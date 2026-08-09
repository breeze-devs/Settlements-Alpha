package dev.breezes.settlements.presentation.ui.hud;

import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Answers what the held-item HUD should show for a given held stack.
 * <p>
 * Absence means this provider has nothing to say — not every provider recognizes every item,
 * and the HUD renders nothing at all when every provider returns empty.
 */
@ClientSide
public interface HeldItemHudProvider {

    Optional<HeldItemHudContent> contentFor(@Nonnull ItemStack heldStack);

}
