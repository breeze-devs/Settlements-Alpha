package dev.breezes.settlements.application.ai.behavior.usecases.villager.smelting.blastore;

import dev.breezes.settlements.domain.ai.conditions.ICondition;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.domain.smelting.catalog.BlastOreRecipe;
import dev.breezes.settlements.domain.smelting.catalog.BlastOreRecipeRegistry;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Determines whether at least one blast-furnace recipe can be executed from villager inventory,
 * and caches all currently valid recipes for behavior start-time selection.
 */
public class BlastRecipeAvailableCondition implements ICondition<BaseVillager> {

    private final BlastOreRecipeRegistry recipeRegistry;

    @Getter
    private List<BlastOreRecipe> validRecipes;

    public BlastRecipeAvailableCondition(@Nonnull BlastOreRecipeRegistry recipeRegistry) {
        this.recipeRegistry = recipeRegistry;
        this.validRecipes = List.of();
    }

    @Override
    public boolean test(@Nullable BaseVillager villager) {
        this.validRecipes = List.of();
        if (villager == null) {
            return false;
        }

        VillagerInventory inventory = villager.getSettlementsInventory();
        this.validRecipes = this.recipeRegistry.allRecipes().stream()
                .filter(recipe -> {
                    Item item = BuiltInRegistries.ITEM.get(recipe.input());
                    return item != Items.AIR && inventory.count(item) >= recipe.inputCount();
                })
                .toList();
        return !this.validRecipes.isEmpty();
    }

}
