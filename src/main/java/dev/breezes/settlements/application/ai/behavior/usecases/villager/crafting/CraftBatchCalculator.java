package dev.breezes.settlements.application.ai.behavior.usecases.villager.crafting;

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
import lombok.AllArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;

/**
 * Joins a {@link CraftRecipe} with the villager's inventory and the trade {@code StockPolicy} ladder
 * to decide how many units may be crafted this tick. Shared by the availability condition (craftable
 * iff batch >= 1) and the craft step (which recomputes live after arrival), so both agree.
 * <p>
 * The clamp is: expertise level, floored by inventory headroom above each input's economic reserve,
 * and floored by the output's overflow ceiling. An output with no {@code dump} rung has no ceiling
 * and is therefore not craftable — that is intentional (future-proof recipes ship idle).
 */
@AllArgsConstructor(onConstructor_ = @Inject)
public class CraftBatchCalculator {

    private final TradeCatalogRegistry tradeCatalog;

    /**
     * Returns the number of units craftable this tick.
     * <p>
     * Returns 0 when the recipe is not economically or physically craftable.
     */
    public int computeBatch(@Nonnull BaseVillager villager, @Nonnull CraftRecipe recipe) {
        Item outputItem = BuiltInRegistries.ITEM.get(recipe.output().itemId());
        VillagerProfessionKey profession = villager.getProfession();

        Integer ceiling = this.dumpAbove(profession, outputItem);
        if (ceiling == null) {
            // No overflow line configured, not craftable yet
            return 0;
        }

        VillagerInventory inventory = villager.getSettlementsInventory();
        List<CraftIngredient> inputs = recipe.inputs();
        int[] inputHeadrooms = new int[inputs.size()];
        int[] inputCounts = new int[inputs.size()];
        for (int i = 0; i < inputs.size(); i++) {
            CraftIngredient input = inputs.get(i);
            inputHeadrooms[i] = inventory.countMatching(input.match()) - this.reserveFor(profession, input.match());
            inputCounts[i] = input.count();
        }

        return clampBatch(villager.getExpertise().getLevel(),
                ceiling - inventory.count(outputItem),
                recipe.output().count(),
                inputHeadrooms,
                inputCounts);
    }

    /**
     * The pure batch clamp: expertise, capped by the output's ceiling headroom and by each input's
     * reserve headroom, all divided by their per-unit costs. Extracted so the divisor arithmetic — the
     * error-prone part (each per-unit cost must divide its own headroom) — is unit-testable with plain
     * ints. A non-positive headroom yields a non-positive term, so the final {@code max(.., 0)} reports
     * "not craftable" regardless of truncation direction.
     */
    static int clampBatch(int expertiseLevel, int outputHeadroom, int outputCount,
                          @Nonnull int[] inputHeadrooms, @Nonnull int[] inputCounts) {
        int batch = Math.min(expertiseLevel, outputHeadroom / outputCount);
        for (int i = 0; i < inputHeadrooms.length; i++) {
            batch = Math.min(batch, inputHeadrooms[i] / inputCounts[i]);
        }
        return Math.max(batch, 0);
    }

    /**
     * How far the output sits below its overflow ceiling ({@code dumpAbove - held}). Used to prefer the
     * output the village most lacks. Returns {@link Integer#MIN_VALUE} when the output has no ceiling so
     * such recipes sort last (they are filtered out before selection anyway).
     */
    public int ceilingHeadroom(@Nonnull BaseVillager villager, @Nonnull CraftRecipe recipe) {
        Item outputItem = BuiltInRegistries.ITEM.get(recipe.output().itemId());
        Integer ceiling = this.dumpAbove(villager.getProfession(), outputItem);

        if (ceiling == null) {
            // Unreachable: selection only runs over recipes the availability filter already proved craftable
            return Integer.MIN_VALUE;
        }

        return ceiling - villager.getSettlementsInventory().count(outputItem);
    }

    /**
     * The overflow ceiling for the output item, or {@code null} when no dump rung matches it.
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
     * The quantity of the input the villager holds back from crafting: its sell buffer {@code offer.above}
     * (0 when it does not sell the input). Reserve is deliberately <em>not</em> the restock floor: since the
     * demand evaluator refills only up to {@code restock.below}, folding that floor into the reserve would
     * pin a bought-only input at its buy target and the recipe would never gain headroom. Using the sell
     * buffer instead lets a bought input craft down toward zero (restock then refills it — the normal
     * production loop), while a self-produced-and-sold input still keeps its sell stock intact. Runaway
     * buying is impossible because the output's {@code dump.above} ceiling caps how much is ever crafted.
     */
    private int reserveFor(@Nonnull VillagerProfessionKey profession, @Nonnull ItemMatch inputMatch) {
        return this.tradeCatalog.findOffers(profession, inputMatch).stream()
                .filter(entry -> coversInput(entry.match(), inputMatch))
                .mapToInt(OfferEntry::surplusThreshold)
                .max()
                .orElse(0);
    }

    /**
     * Whether a catalog rung's match genuinely applies to the input. {@code findOffers} also returns
     * cross-kind candidates (e.g. the universal {@code c:foods} rung against an iron input) at priority 0
     * for a scanner to resolve; we resolve them here by real tag membership so an unrelated tag rung does
     * not inflate the reserve.
     */
    private static boolean coversInput(@Nonnull ItemMatch rungMatch, @Nonnull ItemMatch inputMatch) {
        if (rungMatch.equals(inputMatch)) {
            return true;
        }
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
