package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.domain.crafting.catalog.CraftRecipe;
import dev.breezes.settlements.domain.economy.catalog.ItemMatches;
import dev.breezes.settlements.domain.economy.catalog.SupplyEntry;
import dev.breezes.settlements.domain.economy.catalog.TradeCatalogRegistry;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.infrastructure.minecraft.data.crafting.CraftCatalogDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.forge.ForgeCatalogDataManager;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;

/**
 * Boot-time coherence check for the crafting datapacks: warns about any recipe whose output has no
 * matching {@code dump} rung in the trade catalog. Such a recipe is silently never craftable.
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
@CustomLog
public final class RecipeCatalogValidationReloadListener extends SimplePreparableReloadListener<Void> {

    private final CraftCatalogDataManager craftCatalog;
    private final ForgeCatalogDataManager forgeCatalog;
    private final TradeCatalogRegistry tradeCatalog;

    @Override
    protected Void prepare(@Nonnull ResourceManager resourceManager, @Nonnull ProfilerFiller profiler) {
        return null;
    }

    @Override
    protected void apply(Void ignored, @Nonnull ResourceManager resourceManager, @Nonnull ProfilerFiller profiler) {
        int missing = this.validateCatalog("Craft", this.craftCatalog.loadedRecipes())
                + this.validateCatalog("Forge", this.forgeCatalog.loadedRecipes());

        if (missing > 0) {
            log.warn("Recipe catalog validation: {} recipe output(s) lack a trade-catalog dump ceiling and ship idle", missing);
        }
    }

    private int validateCatalog(@Nonnull String catalogLabel, @Nonnull Map<VillagerProfessionKey, List<CraftRecipe>> recipesByProfession) {
        int missing = 0;
        for (Map.Entry<VillagerProfessionKey, List<CraftRecipe>> entry : recipesByProfession.entrySet()) {
            VillagerProfessionKey profession = entry.getKey();
            for (CraftRecipe recipe : entry.getValue()) {
                if (!this.hasDumpRung(profession, recipe)) {
                    missing++;
                    log.warn("{} recipe '{}' ({}) outputs '{}', which has no dump rung in the trade catalog"
                                    + " — it will never be craftable until a dump rung is added",
                            catalogLabel, recipe.id(), profession.id(), recipe.output().itemId());
                }
            }
        }
        return missing;
    }

    private boolean hasDumpRung(@Nonnull VillagerProfessionKey profession, @Nonnull CraftRecipe recipe) {
        ItemStack outputStack = new ItemStack(BuiltInRegistries.ITEM.get(recipe.output().itemId()));
        for (SupplyEntry entry : this.tradeCatalog.supplyFor(profession)) {
            if (ItemMatches.test(entry.match(), outputStack)) {
                return true;
            }
        }
        return false;
    }

}
