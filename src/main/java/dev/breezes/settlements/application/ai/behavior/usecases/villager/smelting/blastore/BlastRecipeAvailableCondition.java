package dev.breezes.settlements.application.ai.behavior.usecases.villager.smelting.blastore;

import dev.breezes.settlements.domain.ai.conditions.ICondition;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Determines whether at least one blast-furnace recipe can be executed from villager inventory,
 * and caches all currently valid recipes for behavior start-time selection.
 */
public class BlastRecipeAvailableCondition implements ICondition<BaseVillager> {

    private final List<BlastOreRecipe> recipes;

    @Getter
    private List<BlastOreRecipe> validRecipes;

    public BlastRecipeAvailableCondition(@Nonnull List<BlastOreRecipe> recipes) {
        this.recipes = recipes;
        this.validRecipes = List.of();
    }

    @Override
    public boolean test(@Nullable BaseVillager villager) {
        this.validRecipes = List.of();
        if (villager == null) {
            return false;
        }

        VillagerInventory inventory = villager.getSettlementsInventory();
        this.validRecipes = this.recipes.stream()
                .filter(recipe -> inventory.count(recipe.getInput()) >= recipe.getInputCount())
                .toList();
        return !this.validRecipes.isEmpty();
    }

}
