package dev.breezes.settlements.domain.crafting.catalog;

import dev.breezes.settlements.domain.entities.VillagerProfessionKey;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Read contract shared by every recipe catalog keyed by profession (generic crafting-grid recipes,
 * anvil-forged recipes, and future station-specific catalogs). Lets station-agnostic collaborators
 * such as {@code CraftBatchCalculator}'s callers depend on "a recipe source for this profession"
 * without committing to which concrete catalog backs it.
 */
public interface ProfessionRecipeCatalog {

    /**
     * Returns the recipes registered for the given profession, or an empty list when none are.
     */
    List<CraftRecipe> recipesFor(@Nonnull VillagerProfessionKey profession);

}
