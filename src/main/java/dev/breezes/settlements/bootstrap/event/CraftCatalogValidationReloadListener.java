package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.domain.crafting.catalog.CraftRecipe;
import dev.breezes.settlements.domain.economy.catalog.ItemMatches;
import dev.breezes.settlements.domain.economy.catalog.SupplyEntry;
import dev.breezes.settlements.domain.economy.catalog.TradeCatalogRegistry;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.infrastructure.minecraft.data.crafting.CraftCatalogDataManager;
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
 * Boot-time coherence check for the crafting datapack: warns about any craft recipe whose output has no
 * matching {@code dump} rung in the trade catalog. Such a recipe is silently never craftable — the economic
 * gate has no overflow ceiling to work against ({@code CraftBatchCalculator} treats a ceiling-less output as
 * a future-proof placeholder) — so a missing or mistyped rung otherwise manifests as a villager that just
 * never crafts, with no diagnostic. Surfacing it here turns that into a one-line startup warning.
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
@CustomLog
public final class CraftCatalogValidationReloadListener extends SimplePreparableReloadListener<Void> {

    private final CraftCatalogDataManager craftCatalog;
    private final TradeCatalogRegistry tradeCatalog;

    @Override
    protected Void prepare(@Nonnull ResourceManager resourceManager, @Nonnull ProfilerFiller profiler) {
        return null;
    }

    @Override
    protected void apply(Void ignored, @Nonnull ResourceManager resourceManager, @Nonnull ProfilerFiller profiler) {
        int missing = 0;
        for (Map.Entry<VillagerProfessionKey, List<CraftRecipe>> entry : this.craftCatalog.loadedRecipes().entrySet()) {
            VillagerProfessionKey profession = entry.getKey();
            for (CraftRecipe recipe : entry.getValue()) {
                if (!this.hasDumpRung(profession, recipe)) {
                    missing++;
                    log.warn("Craft recipe '{}' ({}) outputs '{}', which has no dump rung in the trade catalog"
                                    + " — it will never be craftable until a dump rung is added",
                            recipe.id(), profession.id(), recipe.output().itemId());
                }
            }
        }

        if (missing > 0) {
            log.warn("Craft catalog validation: {} recipe output(s) lack a trade-catalog dump ceiling and ship idle", missing);
        }
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
