package dev.breezes.settlements.di.modules;

import dagger.Module;
import dagger.Provides;
import dev.breezes.settlements.domain.smelting.catalog.BlastOreRecipeRegistry;
import dev.breezes.settlements.infrastructure.minecraft.data.smelting.BlastOreRecipeDataManager;

import javax.inject.Singleton;

/**
 * Exposes the blast-furnace ore catalog domain interface through its infrastructure implementation so
 * callers depend on the stable abstraction rather than the datapack reload listener.
 */
@Module
public final class SmeltingModule {

    @Provides
    @Singleton
    static BlastOreRecipeRegistry blastOreRecipeRegistry(BlastOreRecipeDataManager blastOreRecipeDataManager) {
        return blastOreRecipeDataManager;
    }

}
