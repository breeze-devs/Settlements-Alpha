package dev.breezes.settlements.di.modules.server;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import dev.breezes.settlements.application.settlement.lifecycle.SettlementLifecyclePublisher;
import dev.breezes.settlements.application.settlement.persistence.SettlementStore;
import dev.breezes.settlements.application.settlement.persistence.SettlementStoreListener;
import dev.breezes.settlements.bootstrap.event.SettlementClaimSyncServerEvents;
import dev.breezes.settlements.bootstrap.event.SettlementDirectorySyncServerEvents;
import dev.breezes.settlements.infrastructure.minecraft.event.settlement.NeoForgeSettlementLifecyclePublisher;
import dev.breezes.settlements.infrastructure.minecraft.persistence.SavedDataSettlementStore;

/**
 * Binds {@link SettlementStore} to its implementation backed by per-dimension SavedData and the listeners its changes
 * are announced to, and {@link SettlementLifecyclePublisher} to its implementation posting NeoForge events.
 */
@Module
public abstract class SettlementStoreModule {

    @Binds
    abstract SettlementStore settlementStore(SavedDataSettlementStore implementation);

    @Binds
    abstract SettlementLifecyclePublisher settlementLifecyclePublisher(
            NeoForgeSettlementLifecyclePublisher implementation);

    @Binds
    @IntoSet
    abstract SettlementStoreListener settlementClaimSync(SettlementClaimSyncServerEvents implementation);

    @Binds
    @IntoSet
    abstract SettlementStoreListener settlementDirectorySync(SettlementDirectorySyncServerEvents implementation);

}
