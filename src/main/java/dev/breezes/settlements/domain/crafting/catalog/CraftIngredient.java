package dev.breezes.settlements.domain.crafting.catalog;

import dev.breezes.settlements.domain.economy.catalog.ItemMatch;

/**
 * Required input of a {@link CraftRecipe}. Accepts either a concrete item or a tag (e.g. {@code c:wools})
 */
public record CraftIngredient(
        ItemMatch match,
        int count
) {

    public CraftIngredient {
        if (count < 1) {
            throw new IllegalArgumentException("Craft ingredient count must be at least 1");
        }
    }

}
