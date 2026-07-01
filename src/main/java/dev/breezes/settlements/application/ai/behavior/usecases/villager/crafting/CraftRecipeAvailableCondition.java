package dev.breezes.settlements.application.ai.behavior.usecases.villager.crafting;

import dev.breezes.settlements.domain.ai.conditions.ICondition;
import dev.breezes.settlements.domain.crafting.catalog.CraftCatalogRegistry;
import dev.breezes.settlements.domain.crafting.catalog.CraftRecipe;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Determines whether the villager's profession has at least one economically-craftable recipe from its
 * current inventory, and caches the craftable set for behavior start-time selection.
 */
public class CraftRecipeAvailableCondition implements ICondition<BaseVillager> {

    private final CraftCatalogRegistry craftCatalog;
    private final CraftBatchCalculator batchCalculator;

    @Getter
    private List<CraftRecipe> validRecipes;

    public CraftRecipeAvailableCondition(@Nonnull CraftCatalogRegistry craftCatalog,
                                         @Nonnull CraftBatchCalculator batchCalculator) {
        this.craftCatalog = craftCatalog;
        this.batchCalculator = batchCalculator;
        this.validRecipes = List.of();
    }

    @Override
    public boolean test(@Nullable BaseVillager villager) {
        this.validRecipes = List.of();
        if (villager == null) {
            return false;
        }

        this.validRecipes = this.craftCatalog.recipesFor(villager.getProfession()).stream()
                .filter(recipe -> this.batchCalculator.computeBatch(villager, recipe) >= 1)
                .toList();
        return !this.validRecipes.isEmpty();
    }

}
