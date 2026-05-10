package dev.breezes.settlements.di;

import dagger.Subcomponent;
import dev.breezes.settlements.application.ai.catalog.BehaviorPoolResolver;
import dev.breezes.settlements.application.ai.genetics.PersonalityDeriver;
import dev.breezes.settlements.application.ai.memory.MemoryImportanceGate;
import dev.breezes.settlements.application.ai.trading.TradeSessionRegistry;
import dev.breezes.settlements.application.economy.VillagerWallet;
import dev.breezes.settlements.application.economy.demand.DemandSignalService;
import dev.breezes.settlements.application.settlement.persistence.SettlementMetadataQueueService;
import dev.breezes.settlements.application.ui.bubble.VillagerBubbleService;
import dev.breezes.settlements.bootstrap.event.PlayerSettlementTracker;
import dev.breezes.settlements.bootstrap.event.RegionSubtitleHandler;
import dev.breezes.settlements.bootstrap.event.SettlementMetadataPersistenceServerEvents;
import dev.breezes.settlements.bootstrap.event.UiSyncServerEvents;
import dev.breezes.settlements.di.modules.server.BehaviorCatalogModule;
import dev.breezes.settlements.di.modules.server.InventoryModule;
import dev.breezes.settlements.di.modules.server.PlanningModule;
import dev.breezes.settlements.di.modules.server.PoolModule;
import dev.breezes.settlements.di.modules.server.ServerNetworkModule;
import dev.breezes.settlements.di.modules.server.SettlementQueryModule;
import dev.breezes.settlements.di.modules.server.UiSyncModule;
import dev.breezes.settlements.domain.ai.catalog.IBehaviorCatalog;
import dev.breezes.settlements.domain.ai.planning.IPlanGenerator;
import dev.breezes.settlements.domain.ai.schedule.IWeekCycleProvider;
import dev.breezes.settlements.domain.inventory.GeneticInventoryProvider;
import dev.breezes.settlements.domain.settlement.query.SettlementQueryService;
import dev.breezes.settlements.infrastructure.minecraft.behavior.planning.PlanRunnerBehavior;
import dev.breezes.settlements.infrastructure.minecraft.data.fishing.FishCatchDataManager;
import dev.breezes.settlements.infrastructure.minecraft.query.SettlementStructureLocator;
import dev.breezes.settlements.infrastructure.network.core.ServerSidePacketReceiver;

import javax.inject.Provider;

@ServerScope
@Subcomponent(modules = {
        ServerNetworkModule.class,
        BehaviorCatalogModule.class,
        PoolModule.class,
        PlanningModule.class,
        InventoryModule.class,
        SettlementQueryModule.class,
        UiSyncModule.class,
})
public interface ServerComponent {

    VillagerBubbleService villagerBubbleService();

    IBehaviorCatalog behaviorCatalog();

    BehaviorPoolResolver behaviorPoolResolver();

    IPlanGenerator planGenerator();

    IWeekCycleProvider weekCycleProvider();

    Provider<PlanRunnerBehavior> planRunnerBehaviorProvider();

    MemoryImportanceGate memoryImportanceGate();

    PersonalityDeriver personalityDeriver();

    GeneticInventoryProvider geneticInventoryProvider();

    ServerSidePacketReceiver serverSidePacketReceiver();

    PlayerSettlementTracker playerSettlementTracker();

    RegionSubtitleHandler regionSubtitleHandler();

    SettlementMetadataPersistenceServerEvents settlementMetadataPersistenceServerEvents();

    UiSyncServerEvents uiSyncServerEvents();

    SettlementMetadataQueueService settlementMetadataQueueService();

    VillagerWallet villagerWallet();

    DemandSignalService demandSignalService();

    SettlementQueryService settlementQueryService();

    SettlementStructureLocator settlementStructureLocator();

    TradeSessionRegistry tradeSessionRegistry();

    // Exposed for VillagerFishingHook (non-injectable Minecraft entity, server-only)
    FishCatchDataManager fishCatchDataManager();

    @Subcomponent.Factory
    interface Factory {
        ServerComponent create();
    }

}
