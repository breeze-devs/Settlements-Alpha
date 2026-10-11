package dev.breezes.settlements.application.ai.behavior.usecases.villager.smelting.blastore;

import dev.breezes.settlements.application.economy.supply.SupplyEvaluator;
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
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * Determines whether at least one blast-furnace recipe can run now, and caches the runnable recipes for behavior
 * start-time selection.
 * <p>
 * A recipe naming an unregistered item never runs, nor does one whose output sits at the profession's dump
 * threshold. Recipes whose input the villager holds take precedence; only when there are none, and item requirements
 * are bypassed, do recipes run without their input, and then only those whose output has a dump threshold.
 */
public class BlastRecipeAvailableCondition implements ICondition<BaseVillager> {

    private final BlastOreRecipeRegistry recipeRegistry;
    private final SupplyEvaluator supplyEvaluator;

    @Getter
    private List<BlastOreRecipe> validRecipes;

    public BlastRecipeAvailableCondition(@Nonnull BlastOreRecipeRegistry recipeRegistry,
                                         @Nonnull SupplyEvaluator supplyEvaluator) {
        this.recipeRegistry = recipeRegistry;
        this.supplyEvaluator = supplyEvaluator;
        this.validRecipes = List.of();
    }

    @Override
    public boolean test(@Nullable BaseVillager villager) {
        this.validRecipes = List.of();
        if (villager == null) {
            return false;
        }

        VillagerInventory inventory = villager.getSettlementsInventory();
        List<BlastOreRecipe> withInput = new ArrayList<>();
        List<BlastOreRecipe> cappedWithoutInput = new ArrayList<>();
        for (BlastOreRecipe recipe : this.recipeRegistry.allRecipes()) {
            Item input = BuiltInRegistries.ITEM.get(recipe.input());
            Item output = BuiltInRegistries.ITEM.get(recipe.output());
            if (input == Items.AIR || output == Items.AIR) {
                continue;
            }

            OptionalInt ceiling = this.supplyEvaluator.dumpThreshold(villager.getProfession(), output);
            if (ceiling.isPresent() && ceiling.getAsInt() - inventory.count(output) < recipe.outputCount()) {
                continue;
            }

            if (inventory.count(input) >= recipe.inputCount()) {
                withInput.add(recipe);
            } else if (ceiling.isPresent()) {
                cappedWithoutInput.add(recipe);
            }
        }

        // Held ore first, so a villager spends what it carries. An output with no dump threshold has nothing to bound
        // it once its ore is waived, so it never runs without its ore.
        this.validRecipes = withInput.isEmpty() && inventory.requirementsBypassed()
                ? List.copyOf(cappedWithoutInput)
                : List.copyOf(withInput);
        return !this.validRecipes.isEmpty();
    }

}
