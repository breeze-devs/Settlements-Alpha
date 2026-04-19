package dev.breezes.settlements.di.modules;

import dagger.Module;
import dagger.Provides;
import dev.breezes.settlements.application.ai.trading.NegotiationEngine;
import dev.breezes.settlements.domain.economy.catalog.TradeCatalogRegistry;
import dev.breezes.settlements.infrastructure.minecraft.data.trading.TradeCatalogDataManager;

import javax.inject.Singleton;

/**
 * Exposes economy domain interfaces through their infrastructure implementations so callers
 * depend on stable abstractions rather than infrastructure details.
 */
@Module
public final class EconomyModule {

    @Provides
    @Singleton
    static TradeCatalogRegistry tradeCatalogRegistry(TradeCatalogDataManager tradeCatalogDataManager) {
        return tradeCatalogDataManager;
    }

    @Provides
    @Singleton
    static NegotiationEngine negotiationEngine() {
        return new NegotiationEngine();
    }

}
