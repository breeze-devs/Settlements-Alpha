package dev.breezes.settlements.domain.crafting.catalog;

import dev.breezes.settlements.domain.economy.catalog.ItemMatch;

import javax.annotation.Nonnull;

/**
 * Required input of a {@link CraftRecipe}. Accepts either a concrete item or a tag (e.g. {@code c:wools})
 */
public record CraftIngredient(
        @Nonnull ItemMatch match,
        int count
) {

    public CraftIngredient {
        if (count < 1) {
            throw new IllegalArgumentException("Craft ingredient count must be at least 1");
        }
    }

}
