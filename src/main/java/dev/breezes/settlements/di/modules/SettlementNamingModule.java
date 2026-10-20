package dev.breezes.settlements.di.modules;

import dagger.Binds;
import dagger.Module;
import dev.breezes.settlements.domain.settlement.naming.SettlementNamePoolRegistry;
import dev.breezes.settlements.infrastructure.minecraft.data.naming.SettlementNamePoolDataManager;

/**
 * Binds {@link SettlementNamePoolRegistry} to its datapack-backed implementation.
 */
@Module
public interface SettlementNamingModule {

    @Binds
    SettlementNamePoolRegistry settlementNamePoolRegistry(SettlementNamePoolDataManager dataManager);

}
