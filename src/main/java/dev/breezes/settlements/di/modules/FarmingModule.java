package dev.breezes.settlements.di.modules;

import dagger.Module;
import dagger.Provides;
import dev.breezes.settlements.domain.farming.CultivationCropRegistry;
import dev.breezes.settlements.infrastructure.minecraft.data.farming.crops.CultivationCropDataManager;

import jakarta.inject.Singleton;

/**
 * Exposes the cultivation crop domain interface through its infrastructure implementation so callers
 * depend on the stable abstraction rather than the datapack reload listener.
 */
@Module
public final class FarmingModule {

    @Provides
    @Singleton
    static CultivationCropRegistry cultivationCropRegistry(CultivationCropDataManager cultivationCropDataManager) {
        return cultivationCropDataManager;
    }

}
