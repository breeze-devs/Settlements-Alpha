package dev.breezes.settlements.domain.crafting.catalog;

import dev.breezes.settlements.domain.entities.VillagerProfessionKey;

import javax.annotation.Nonnull;
import java.util.List;

public interface CraftCatalogRegistry {

    /**
     * Returns the recipes registered for the given profession, or an empty list when none are.
     */
    List<CraftRecipe> recipesFor(@Nonnull VillagerProfessionKey profession);

}
