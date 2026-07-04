package dev.breezes.settlements.domain.smelting.catalog;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

/**
 * Domain port over the datapack-driven blast-furnace ore catalog, so callers depend on the stable
 * abstraction rather than the infrastructure reload listener that backs it.
 */
public interface BlastOreRecipeRegistry {

    List<BlastOreRecipe> allRecipes();

    Optional<BlastOreRecipe> forInput(@Nonnull ResourceLocation input);

}
