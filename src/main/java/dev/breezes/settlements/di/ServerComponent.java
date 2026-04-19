package dev.breezes.settlements.di;

import dagger.Subcomponent;
import dev.breezes.settlements.application.ai.trading.TradeSessionRegistry;
import dev.breezes.settlements.application.economy.demand.DemandSignalService;
import dev.breezes.settlements.application.economy.VillagerWallet;
import dev.breezes.settlements.application.ui.behavior.session.BehaviorControllerSessionService;
import dev.breezes.settlements.application.ui.behavior.snapshot.BehaviorControllerSnapshotBuilder;
import dev.breezes.settlements.application.ui.bubble.VillagerBubbleService;
import dev.breezes.settlements.application.ui.stats.session.VillagerStatsSessionService;
import dev.breezes.settlements.application.ui.stats.snapshot.VillagerStatsSnapshotBuilder;
import dev.breezes.settlements.bootstrap.event.BehaviorControllerServerEvents;
import dev.breezes.settlements.bootstrap.event.VillagerStatsServerEvents;
import dev.breezes.settlements.di.behavior.BehaviorPackageResolver;
import dev.breezes.settlements.di.modules.server.BehaviorModule;
import dev.breezes.settlements.di.modules.server.ServerNetworkModule;
import dev.breezes.settlements.infrastructure.minecraft.data.fishing.FishCatchDataManager;
import dev.breezes.settlements.infrastructure.network.core.ServerSidePacketReceiver;

@ServerScope
@Subcomponent(modules = {
        ServerNetworkModule.class,
        BehaviorModule.class,
})
public interface ServerComponent {

    BehaviorControllerSessionService behaviorControllerSessionService();

    VillagerStatsSessionService villagerStatsSessionService();

    VillagerBubbleService villagerBubbleService();

    BehaviorControllerSnapshotBuilder behaviorControllerSnapshotBuilder();

    VillagerStatsSnapshotBuilder villagerStatsSnapshotBuilder();

    BehaviorPackageResolver behaviorPackageResolver();

    ServerSidePacketReceiver serverSidePacketReceiver();

    BehaviorControllerServerEvents behaviorControllerServerEvents();

    VillagerStatsServerEvents villagerStatsServerEvents();

    VillagerWallet villagerWallet();

    DemandSignalService demandSignalService();

    TradeSessionRegistry tradeSessionRegistry();

    // Exposed for VillagerFishingHook (non-injectable Minecraft entity, server-only)
    FishCatchDataManager fishCatchDataManager();

    @Subcomponent.Factory
    interface Factory {
        ServerComponent create();
    }

}
