package dev.breezes.settlements.di.modules;

import dagger.Module;
import dagger.Provides;
import dev.breezes.settlements.application.enchanting.engine.EnchantmentEngine;
import dev.breezes.settlements.infrastructure.minecraft.data.enchanting.EnchantmentCostDataManager;

import javax.inject.Singleton;

@Module
public final class BehaviorServicesModule {

    @Provides
    @Singleton
    static EnchantmentEngine enchantmentEngine(EnchantmentCostDataManager costDataManager) {
        return new EnchantmentEngine(costDataManager);
    }

}
