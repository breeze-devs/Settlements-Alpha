package dev.breezes.settlements.di.modules.server;

import dagger.Module;
import dagger.Provides;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.inventory.GeneticInventoryProvider;

@Module
public final class InventoryModule {

    @Provides
    @ServerScope
    static GeneticInventoryProvider geneticInventoryProvider() {
        return new GeneticInventoryProvider();
    }

}
