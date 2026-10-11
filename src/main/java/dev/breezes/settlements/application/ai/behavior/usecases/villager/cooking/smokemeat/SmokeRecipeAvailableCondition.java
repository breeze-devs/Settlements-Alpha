package dev.breezes.settlements.application.ai.behavior.usecases.villager.cooking.smokemeat;

import dev.breezes.settlements.application.economy.supply.SupplyEvaluator;
import dev.breezes.settlements.domain.ai.conditions.ICondition;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * Determines whether at least one smoking recipe can run now, and caches the runnable recipes for behavior
 * start-time selection.
 * <p>
 * A recipe never runs while its output sits at the profession's dump threshold. Recipes whose input the villager
 * holds take precedence; only when there are none, and item requirements are bypassed, do recipes run without their
 * input, and then only those whose output has a dump threshold.
 */
public class SmokeRecipeAvailableCondition implements ICondition<BaseVillager> {

    private final List<SmokeMeatRecipe> recipes;
    private final SupplyEvaluator supplyEvaluator;

    @Getter
    private List<SmokeMeatRecipe> validRecipes;

    public SmokeRecipeAvailableCondition(@Nonnull List<SmokeMeatRecipe> recipes,
                                         @Nonnull SupplyEvaluator supplyEvaluator) {
        this.recipes = recipes;
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
        List<SmokeMeatRecipe> withInput = new ArrayList<>();
        List<SmokeMeatRecipe> cappedWithoutInput = new ArrayList<>();
        for (SmokeMeatRecipe recipe : this.recipes) {
            OptionalInt ceiling = this.supplyEvaluator.dumpThreshold(villager.getProfession(), recipe.getOutput());
            if (ceiling.isPresent()
                    && ceiling.getAsInt() - inventory.count(recipe.getOutput()) < recipe.getOutputCount()) {
                continue;
            }

            if (inventory.count(recipe.getInput()) >= recipe.getInputCount()) {
                withInput.add(recipe);
            } else if (ceiling.isPresent()) {
                cappedWithoutInput.add(recipe);
            }
        }

        // Held input first, so a villager spends what it carries. An output with no dump threshold has nothing to
        // bound it once its input is waived, so it never runs without its input.
        this.validRecipes = withInput.isEmpty() && inventory.requirementsBypassed()
                ? List.copyOf(cappedWithoutInput)
                : List.copyOf(withInput);
        return !this.validRecipes.isEmpty();
    }

}
