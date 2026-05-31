package dev.breezes.settlements.application.ai.behavior.usecases.villager.cooking.smokemeat;

import dev.breezes.settlements.domain.ai.conditions.ICondition;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Determines whether at least one smoking recipe can be executed from villager inventory,
 * and caches all currently valid recipes for behavior start-time selection.
 */
public class SmokeRecipeAvailableCondition implements ICondition<BaseVillager> {

    private final List<SmokeMeatRecipe> recipes;

    @Getter
    private List<SmokeMeatRecipe> validRecipes;

    public SmokeRecipeAvailableCondition(@Nonnull List<SmokeMeatRecipe> recipes) {
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
