package dev.breezes.settlements.di.modules;

import dagger.Module;
import dagger.Provides;
import dev.breezes.settlements.domain.crafting.catalog.CraftCatalogRegistry;
import dev.breezes.settlements.infrastructure.minecraft.data.crafting.CraftCatalogDataManager;

import javax.inject.Singleton;

/**
 * Exposes the crafting domain interface through its infrastructure implementation so callers depend on
 * the stable abstraction rather than the datapack reload listener. Kept separate from {@link EconomyModule}
 * to keep generic crafting distinct from the trade economy.
 */
@Module
public final class CraftingModule {

    @Provides
    @Singleton
    static CraftCatalogRegistry craftCatalogRegistry(CraftCatalogDataManager craftCatalogDataManager) {
        return craftCatalogDataManager;
    }

}
