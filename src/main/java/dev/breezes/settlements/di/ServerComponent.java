package dev.breezes.settlements.di;

import dagger.Subcomponent;
import dev.breezes.settlements.application.ai.behavior.usecases.wolf.walkdog.WolfWalkConfig;
import dev.breezes.settlements.application.ai.catalog.BehaviorPoolResolver;
import dev.breezes.settlements.application.ai.courtship.CourtshipSessionRegistry;
import dev.breezes.settlements.application.ai.dialogue.DialogueConfig;
import dev.breezes.settlements.application.ai.dialogue.DialogueProvider;
import dev.breezes.settlements.application.ai.genetics.PersonalityDeriver;
import dev.breezes.settlements.application.ai.gossip.GossipSessionRegistry;
import dev.breezes.settlements.application.ai.inference.InferenceTransport;
import dev.breezes.settlements.application.ai.inference.monologue.MonologueRequestAssembler;
import dev.breezes.settlements.application.ai.memory.MemoryImportanceGate;
import dev.breezes.settlements.application.ai.perception.PerceptionPipeline;
import dev.breezes.settlements.application.ai.socialcue.SocialCueArbiter;
import dev.breezes.settlements.application.ai.trading.TradeSessionRegistry;
import dev.breezes.settlements.application.economy.VillagerWallet;
import dev.breezes.settlements.application.economy.demand.DemandSignalService;
import dev.breezes.settlements.application.settlement.persistence.SettlementMetadataQueueService;
import dev.breezes.settlements.application.ui.bubble.VillagerBubbleService;
import dev.breezes.settlements.bootstrap.event.CourtshipSessionReaperServerEvents;
import dev.breezes.settlements.bootstrap.event.CredibilityDecayServerEvents;
import dev.breezes.settlements.bootstrap.event.GossipSessionReaperServerEvents;
import dev.breezes.settlements.bootstrap.event.PlayerSettlementTracker;
import dev.breezes.settlements.bootstrap.event.RegionSubtitleHandler;
import dev.breezes.settlements.bootstrap.event.RehearsedDialogueSweepServerEvents;
import dev.breezes.settlements.bootstrap.event.SettlementMetadataPersistenceServerEvents;
import dev.breezes.settlements.bootstrap.event.TradeSessionReaperServerEvents;
import dev.breezes.settlements.bootstrap.event.UiSyncServerEvents;
import dev.breezes.settlements.bootstrap.event.VillageAnimalSpawnerServerEvents;
import dev.breezes.settlements.bootstrap.event.VillagerZombificationServerEvents;
import dev.breezes.settlements.bootstrap.event.WorldgenVillagerReplacementServerEvents;
import dev.breezes.settlements.bootstrap.event.WorldEventBusReaperServerEvents;
import dev.breezes.settlements.di.catalog.VillagerSensorFactory;
import dev.breezes.settlements.di.modules.server.BehaviorCatalogModule;
import dev.breezes.settlements.di.modules.server.CredibilityModule;
import dev.breezes.settlements.di.modules.server.DialogueServiceModule;
import dev.breezes.settlements.di.modules.server.GossipModule;
import dev.breezes.settlements.di.modules.server.OverridePolicyModule;
import dev.breezes.settlements.di.modules.server.PerceptionModule;
import dev.breezes.settlements.di.modules.server.PlanningModule;
import dev.breezes.settlements.di.modules.server.PoolModule;
import dev.breezes.settlements.di.modules.server.SensorCatalogModule;
import dev.breezes.settlements.di.modules.server.ServerNetworkModule;
import dev.breezes.settlements.di.modules.server.SettlementQueryModule;
import dev.breezes.settlements.di.modules.server.SocialCueCatalogModule;
import dev.breezes.settlements.di.modules.server.UiSyncModule;
import dev.breezes.settlements.di.modules.server.WorldEventModule;
import dev.breezes.settlements.domain.ai.catalog.IBehaviorCatalog;
import dev.breezes.settlements.domain.ai.eventlane.EventLaneConfig;
import dev.breezes.settlements.domain.ai.planning.IPlanGenerator;
import dev.breezes.settlements.domain.ai.schedule.IWeekCycleProvider;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventBus;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventEmitter;
import dev.breezes.settlements.domain.settlement.query.SettlementQueryService;
import dev.breezes.settlements.infrastructure.minecraft.behavior.planning.PlanRunnerBehavior;
import dev.breezes.settlements.infrastructure.minecraft.data.fishing.FishCatchDataManager;
import dev.breezes.settlements.infrastructure.minecraft.query.SettlementStructureLocator;
import dev.breezes.settlements.infrastructure.network.core.ServerSidePacketReceiver;
import dev.breezes.settlements.shared.util.ReputationUtil;

import javax.inject.Provider;
import java.util.Set;
import java.util.concurrent.ExecutorService;

@ServerScope
@Subcomponent(modules = {
        ServerNetworkModule.class,
        BehaviorCatalogModule.class,
        PoolModule.class,
        PlanningModule.class,
        SettlementQueryModule.class,
        SensorCatalogModule.class,
        UiSyncModule.class,
        SocialCueCatalogModule.class,
        WorldEventModule.class,
        PerceptionModule.class,
        GossipModule.class,
        CredibilityModule.class,
        DialogueServiceModule.class,
        OverridePolicyModule.class,
})
public interface ServerComponent {

    VillagerBubbleService villagerBubbleService();

    SocialCueArbiter socialCueArbiter();

    IBehaviorCatalog behaviorCatalog();

    BehaviorPoolResolver behaviorPoolResolver();

    IPlanGenerator planGenerator();

    IWeekCycleProvider weekCycleProvider();

    Provider<PlanRunnerBehavior> planRunnerBehaviorProvider();

    MemoryImportanceGate memoryImportanceGate();

    PersonalityDeriver personalityDeriver();

    ServerSidePacketReceiver serverSidePacketReceiver();

    PlayerSettlementTracker playerSettlementTracker();

    RegionSubtitleHandler regionSubtitleHandler();

    SettlementMetadataPersistenceServerEvents settlementMetadataPersistenceServerEvents();

    UiSyncServerEvents uiSyncServerEvents();

    CourtshipSessionReaperServerEvents courtshipSessionReaperServerEvents();

    WorldEventBusReaperServerEvents worldEventBusReaperServerEvents();

    WorldEventBus worldEventBus();

    WorldEventEmitter worldEventEmitter();

    PerceptionPipeline perceptionPipeline();

    SettlementMetadataQueueService settlementMetadataQueueService();

    VillagerWallet villagerWallet();

    DemandSignalService demandSignalService();

    SettlementQueryService settlementQueryService();

    SettlementStructureLocator settlementStructureLocator();

    TradeSessionRegistry tradeSessionRegistry();

    CourtshipSessionRegistry courtshipSessionRegistry();

    GossipSessionRegistry gossipSessionRegistry();

    GossipSessionReaperServerEvents gossipSessionReaperServerEvents();

    TradeSessionReaperServerEvents tradeSessionReaperServerEvents();

    ReputationUtil reputationUtil();

    CredibilityDecayServerEvents credibilityDecayServerEvents();

    DialogueProvider dialogueProvider();

    DialogueConfig dialogueConfig();

    InferenceTransport inferenceTransport();

    MonologueRequestAssembler monologueRequestAssembler();

    EventLaneConfig eventLaneConfig();

    RehearsedDialogueSweepServerEvents eveningDialoguePackSweepServerEvents();

    VillagerZombificationServerEvents villagerZombificationServerEvents();

    VillageAnimalSpawnerServerEvents villageAnimalSpawnerServerEvents();

    WorldgenVillagerReplacementServerEvents worldgenVillagerReplacementServerEvents();

    Set<VillagerSensorFactory> villagerSensorFactories();

    ExecutorService planGenerationExecutor();

    // Exposed for VillagerFishingHook (non-injectable Minecraft entity, server-only)
    FishCatchDataManager fishCatchDataManager();

    // Exposed for SettlementsWolf (non-injectable Minecraft entity, server-only)
    WolfWalkConfig wolfWalkConfig();

    @Subcomponent.Factory
    interface Factory {
        ServerComponent create();
    }

}
