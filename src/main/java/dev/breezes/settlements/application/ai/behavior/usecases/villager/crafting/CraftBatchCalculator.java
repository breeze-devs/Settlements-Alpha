package dev.breezes.settlements.application.ai.behavior.usecases.villager.crafting;

import dev.breezes.settlements.application.economy.VillagerWallet;
import dev.breezes.settlements.domain.crafting.catalog.CraftIngredient;
import dev.breezes.settlements.domain.crafting.catalog.CraftRecipe;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.economy.catalog.ItemMatches;
import dev.breezes.settlements.domain.economy.catalog.OfferEntry;
import dev.breezes.settlements.domain.economy.catalog.SupplyEntry;
import dev.breezes.settlements.domain.economy.catalog.TradeCatalogRegistry;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import jakarta.inject.Inject;
import lombok.AllArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

/**
 * Calculates craftable batches and ranks recipes by output stock headroom.
 */
@AllArgsConstructor(onConstructor_ = @Inject)
public class CraftBatchCalculator {

    private final TradeCatalogRegistry tradeCatalog;
    private final VillagerWallet wallet;

    /**
     * Returns the number of recipe executions allowed by factors like expertise, input stock above sell reserves,
     * emerald affordability, and remaining output capacity.
     * <p>
     * Returns 0 when no execution fits or no output overflow ceiling is configured.
     */
    public int computeBatch(@Nonnull BaseVillager villager, @Nonnull CraftRecipe recipe) {
        Item outputItem = BuiltInRegistries.ITEM.get(recipe.output().itemId());
        VillagerProfessionKey profession = villager.getProfession();

        Integer ceiling = this.dumpAbove(profession, outputItem);
        if (ceiling == null) {
            // Recipes are disabled until an overflow policy bounds their production
            return 0;
        }

        VillagerInventory inventory = villager.getSettlementsInventory();
        List<CraftIngredient> inputs = recipe.inputs();
        boolean costsEmeralds = recipe.emeralds() > 0;
        int costCount = costsEmeralds ? inputs.size() + 1 : inputs.size();
        int[] inputHeadrooms = new int[costCount];
        int[] inputCounts = new int[costCount];
        for (int i = 0; i < inputs.size(); i++) {
            CraftIngredient input = inputs.get(i);
            inputHeadrooms[i] = inventory.countMatching(input.match()) - this.reserveFor(profession, input.match());
            inputCounts[i] = input.count();
        }
        if (costsEmeralds) {
            // Crafting reserves no emeralds; affordability and output capacity bound spending per batch.
            inputHeadrooms[inputs.size()] = this.wallet.getBalance(villager);
            inputCounts[inputs.size()] = recipe.emeralds();
        }

        return clampBatch(villager.getExpertise().getLevel(),
                ceiling - inventory.count(outputItem),
                recipe.output().count(),
                inputHeadrooms,
                inputCounts);
    }

    /**
     * Returns a nonnegative batch bounded by expertise and available input and output headroom.
     * <p>
     * Counts are positive quantities per recipe execution; input arrays have equal lengths and pair
     * each headroom with its corresponding cost.
     */
    static int clampBatch(int expertiseLevel, int outputHeadroom, int outputCount,
                          @Nonnull int[] inputHeadrooms, @Nonnull int[] inputCounts) {
        int batch = Math.min(expertiseLevel, outputHeadroom / outputCount);
        for (int i = 0; i < inputHeadrooms.length; i++) {
            batch = Math.min(batch, inputHeadrooms[i] / inputCounts[i]);
        }
        // Stock below reserve or above the output ceiling means no craft, never a negative batch.
        return Math.max(batch, 0);
    }

    /**
     * Returns the output's overflow ceiling minus the villager's held stock, or
     * {@link Integer#MIN_VALUE} when no ceiling is configured.
     */
    public int ceilingHeadroom(@Nonnull BaseVillager villager, @Nonnull CraftRecipe recipe) {
        Item outputItem = BuiltInRegistries.ITEM.get(recipe.output().itemId());
        Integer ceiling = this.dumpAbove(villager.getProfession(), outputItem);

        if (ceiling == null) {
            return Integer.MIN_VALUE;
        }

        return ceiling - villager.getSettlementsInventory().count(outputItem);
    }

    /**
     * Selects the recipe with the largest {@link #ceilingHeadroom}, breaking ties by higher priority.
     * The list must be non-empty and contain only craftable recipes.
     */
    public CraftRecipe selectPreferred(@Nonnull BaseVillager villager, @Nonnull List<CraftRecipe> craftableRecipes) {
        return craftableRecipes.stream()
                .max(Comparator
                        .comparingInt((CraftRecipe recipe) -> this.ceilingHeadroom(villager, recipe))
                        .thenComparingInt(CraftRecipe::priority))
                .orElseThrow();
    }

    /**
     * Returns the highest matching output overflow ceiling, or null when none is configured.
     */
    @Nullable
    private Integer dumpAbove(@Nonnull VillagerProfessionKey profession, @Nonnull Item outputItem) {
        ItemStack outputStack = new ItemStack(outputItem);
        Integer ceiling = null;

        for (SupplyEntry entry : this.tradeCatalog.supplyFor(profession)) {
            if (ItemMatches.test(entry.match(), outputStack)) {
                ceiling = ceiling == null ? entry.dumpAbove() : Math.max(ceiling, entry.dumpAbove());
            }
        }

        return ceiling;
    }

    /**
     * Returns the largest matching input sell buffer, or 0 when no offer applies.
     */
    private int reserveFor(@Nonnull VillagerProfessionKey profession, @Nonnull ItemMatch inputMatch) {
        // Preserve sell stock, but leave bought-only inputs consumable down to zero. Reserving the
        // restock target could strand purchased inputs at that target with no crafting headroom.
        return this.tradeCatalog.findOffers(profession, inputMatch).stream()
                .filter(entry -> coversInput(entry.match(), inputMatch))
                .mapToInt(OfferEntry::surplusThreshold)
                .max()
                .orElse(0);
    }

    /**
     * Accepts identical matches or an item/tag pair whose item belongs to the tag.
     */
    private static boolean coversInput(@Nonnull ItemMatch rungMatch, @Nonnull ItemMatch inputMatch) {
        if (rungMatch.equals(inputMatch)) {
            return true;
        }
        // Mixed item/tag pairs need membership checks so unrelated candidates cannot inflate reserves.
        if (rungMatch instanceof ItemMatch.TagRef tagRef && inputMatch instanceof ItemMatch.ItemRef itemRef) {
            return itemInTag(itemRef, tagRef);
        }
        if (rungMatch instanceof ItemMatch.ItemRef itemRef && inputMatch instanceof ItemMatch.TagRef tagRef) {
            return itemInTag(itemRef, tagRef);
        }
        return false;
    }

    private static boolean itemInTag(@Nonnull ItemMatch.ItemRef itemRef, @Nonnull ItemMatch.TagRef tagRef) {
        return new ItemStack(BuiltInRegistries.ITEM.get(itemRef.id())).is(tagRef.tag());
    }

}
