package dev.breezes.settlements.di;

import dagger.Subcomponent;
import dev.breezes.settlements.application.ai.behavior.usecases.wolf.walkdog.WolfWalkConfig;
import dev.breezes.settlements.application.ai.catalog.BehaviorPoolResolver;
import dev.breezes.settlements.application.ai.courtship.CourtshipSessionRegistry;
import dev.breezes.settlements.application.ai.dialogue.DialogueConfig;
import dev.breezes.settlements.application.ai.dialogue.DialogueProvider;
import dev.breezes.settlements.application.ai.dialogue.RehearsedDialogueConfig;
import dev.breezes.settlements.application.ai.gossip.GossipSessionRegistry;
import dev.breezes.settlements.application.ai.inference.InferenceGate;
import dev.breezes.settlements.application.ai.inference.InferenceTransport;
import dev.breezes.settlements.application.ai.inference.monologue.MonologueRequestAssembler;
import dev.breezes.settlements.application.ai.inference.persona.PersonaRequestAssembler;
import dev.breezes.settlements.application.ai.memory.MemoryAdmissionGate;
import dev.breezes.settlements.application.ai.memory.SensedSiteReader;
import dev.breezes.settlements.application.ai.perception.PerceptionPipeline;
import dev.breezes.settlements.application.ai.persona.PersonaConfig;
import dev.breezes.settlements.application.ai.persona.PersonaGenerationService;
import dev.breezes.settlements.application.ai.planning.PlanRequestService;
import dev.breezes.settlements.application.ai.sensors.WorldResourceIndex;
import dev.breezes.settlements.application.ai.socialcue.SocialCueArbiter;
import dev.breezes.settlements.application.ai.threat.ThreatAssessor;
import dev.breezes.settlements.application.ai.trading.TradeSessionRegistry;
import dev.breezes.settlements.application.economy.VillagerWallet;
import dev.breezes.settlements.application.economy.demand.DemandSignalService;
import dev.breezes.settlements.application.settlement.founding.SettlementFoundingService;
import dev.breezes.settlements.application.settlement.health.SettlementHealthService;
import dev.breezes.settlements.application.settlement.membership.SettlementAssignmentService;
import dev.breezes.settlements.application.settlement.membership.SettlementMembershipService;
import dev.breezes.settlements.application.settlement.naming.SettlementNamer;
import dev.breezes.settlements.application.settlement.persistence.SettlementStore;
import dev.breezes.settlements.application.settlement.query.SettlementQueryService;
import dev.breezes.settlements.application.ui.bubble.VillagerBubbleService;
import dev.breezes.settlements.bootstrap.event.CourtshipSessionReaperServerEvents;
import dev.breezes.settlements.bootstrap.event.CultivationSeedSyncServerEvents;
import dev.breezes.settlements.bootstrap.event.GeneratedSettlementRecordServerEvents;
import dev.breezes.settlements.bootstrap.event.GossipSessionReaperServerEvents;
import dev.breezes.settlements.bootstrap.event.PersonaSweepServerEvents;
import dev.breezes.settlements.bootstrap.event.PlanOverlayPumpServerEvents;
import dev.breezes.settlements.bootstrap.event.PlayerSettlementTracker;
import dev.breezes.settlements.bootstrap.event.RehearsedDialogueSweepServerEvents;
import dev.breezes.settlements.bootstrap.event.ResourceIndexRefresherServerEvents;
import dev.breezes.settlements.bootstrap.event.SettlementArrivalAnnouncer;
import dev.breezes.settlements.bootstrap.event.SettlementClaimSyncServerEvents;
import dev.breezes.settlements.bootstrap.event.SettlementDirectorySyncServerEvents;
import dev.breezes.settlements.bootstrap.event.SettlementHealthServerEvents;
import dev.breezes.settlements.bootstrap.event.TradeSessionReaperServerEvents;
import dev.breezes.settlements.bootstrap.event.UiSyncServerEvents;
import dev.breezes.settlements.bootstrap.event.VillageAnimalSpawnerServerEvents;
import dev.breezes.settlements.bootstrap.event.VillageDayTypeSyncServerEvents;
import dev.breezes.settlements.bootstrap.event.VillagerConversionServerEvents;
import dev.breezes.settlements.bootstrap.event.WorldEventBusReaperServerEvents;
import dev.breezes.settlements.bootstrap.event.WorldgenVillagerReplacementServerEvents;
import dev.breezes.settlements.di.catalog.VillagerSensorFactory;
import dev.breezes.settlements.di.modules.server.BehaviorCatalogModule;
import dev.breezes.settlements.di.modules.server.CombatOptionModule;
import dev.breezes.settlements.di.modules.server.ConcurrencyModule;
import dev.breezes.settlements.di.modules.server.DialogueServiceModule;
import dev.breezes.settlements.di.modules.server.GossipModule;
import dev.breezes.settlements.di.modules.server.NamingModule;
import dev.breezes.settlements.di.modules.server.OverridePolicyModule;
import dev.breezes.settlements.di.modules.server.PerceptionModule;
import dev.breezes.settlements.di.modules.server.PlanInferenceModule;
import dev.breezes.settlements.di.modules.server.PlanningModule;
import dev.breezes.settlements.di.modules.server.PoolModule;
import dev.breezes.settlements.di.modules.server.SensorCatalogModule;
import dev.breezes.settlements.di.modules.server.ServerNetworkModule;
import dev.breezes.settlements.di.modules.server.SettlementQueryModule;
import dev.breezes.settlements.di.modules.server.SettlementStoreModule;
import dev.breezes.settlements.di.modules.server.SocialCueCatalogModule;
import dev.breezes.settlements.di.modules.server.SpeechModule;
import dev.breezes.settlements.di.modules.server.UiSyncModule;
import dev.breezes.settlements.di.modules.server.WorldEventModule;
import dev.breezes.settlements.domain.ai.catalog.IBehaviorCatalog;
import dev.breezes.settlements.domain.ai.naming.VillagerNameDirectory;
import dev.breezes.settlements.domain.ai.planning.IPlanGenerator;
import dev.breezes.settlements.domain.ai.schedule.IWeekCycleProvider;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventBus;
import dev.breezes.settlements.infrastructure.minecraft.behavior.planning.PlanRunnerBehavior;
import dev.breezes.settlements.infrastructure.minecraft.data.fishing.FishCatchDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.mining.OreRegenDataManager;
import dev.breezes.settlements.infrastructure.minecraft.query.SettlementStructureLocator;
import dev.breezes.settlements.infrastructure.network.core.ServerSidePacketReceiver;

import jakarta.inject.Provider;

import java.util.Set;
import java.util.concurrent.ExecutorService;

@ServerScope
@Subcomponent(modules = {
        ServerNetworkModule.class,
        BehaviorCatalogModule.class,
        PoolModule.class,
        PlanningModule.class,
        PlanInferenceModule.class,
        ConcurrencyModule.class,
        SettlementQueryModule.class,
        SettlementStoreModule.class,
        SensorCatalogModule.class,
        UiSyncModule.class,
        SocialCueCatalogModule.class,
        SpeechModule.class,
        WorldEventModule.class,
        PerceptionModule.class,
        GossipModule.class,
        DialogueServiceModule.class,
        OverridePolicyModule.class,
        NamingModule.class,
        CombatOptionModule.class,
})
public interface ServerComponent {

    VillagerBubbleService villagerBubbleService();

    SocialCueArbiter socialCueArbiter();

    ThreatAssessor threatAssessor();

    IBehaviorCatalog behaviorCatalog();

    BehaviorPoolResolver behaviorPoolResolver();

    IPlanGenerator planGenerator();

    IWeekCycleProvider weekCycleProvider();

    Provider<PlanRunnerBehavior> planRunnerBehaviorProvider();

    MemoryAdmissionGate memoryAdmissionGate();

    ServerSidePacketReceiver serverSidePacketReceiver();

    PlayerSettlementTracker playerSettlementTracker();

    SettlementArrivalAnnouncer settlementArrivalAnnouncer();

    GeneratedSettlementRecordServerEvents generatedSettlementRecordServerEvents();

    UiSyncServerEvents uiSyncServerEvents();

    CourtshipSessionReaperServerEvents courtshipSessionReaperServerEvents();

    WorldEventBusReaperServerEvents worldEventBusReaperServerEvents();

    ResourceIndexRefresherServerEvents resourceIndexRefresherServerEvents();

    SettlementHealthServerEvents settlementHealthServerEvents();

    SettlementClaimSyncServerEvents settlementClaimSyncServerEvents();

    SettlementDirectorySyncServerEvents settlementDirectorySyncServerEvents();

    WorldEventBus worldEventBus();

    WorldResourceIndex worldResourceIndex();

    PerceptionPipeline perceptionPipeline();

    VillagerWallet villagerWallet();

    DemandSignalService demandSignalService();

    SettlementStore settlementStore();

    SettlementQueryService settlementQueryService();

    SettlementFoundingService settlementFoundingService();

    SettlementHealthService settlementHealthService();

    SettlementNamer settlementNamer();

    SettlementStructureLocator settlementStructureLocator();

    TradeSessionRegistry tradeSessionRegistry();

    CourtshipSessionRegistry courtshipSessionRegistry();

    GossipSessionRegistry gossipSessionRegistry();

    GossipSessionReaperServerEvents gossipSessionReaperServerEvents();

    TradeSessionReaperServerEvents tradeSessionReaperServerEvents();

    DialogueProvider dialogueProvider();

    DialogueConfig dialogueConfig();

    RehearsedDialogueConfig rehearsedDialogueConfig();

    InferenceGate inferenceGate();

    InferenceTransport inferenceTransport();

    MonologueRequestAssembler monologueRequestAssembler();

    PersonaRequestAssembler personaRequestAssembler();

    PersonaConfig personaConfig();

    PersonaGenerationService personaGenerationService();

    SensedSiteReader sensedSiteReader();

    RehearsedDialogueSweepServerEvents eveningDialoguePackSweepServerEvents();

    PlanOverlayPumpServerEvents planOverlayPumpServerEvents();

    PlanRequestService planRequestService();

    PersonaSweepServerEvents personaSweepServerEvents();

    VillagerConversionServerEvents villagerConversionServerEvents();

    VillageAnimalSpawnerServerEvents villageAnimalSpawnerServerEvents();

    WorldgenVillagerReplacementServerEvents worldgenVillagerReplacementServerEvents();

    CultivationSeedSyncServerEvents cultivationSeedSyncServerEvents();

    VillageDayTypeSyncServerEvents villageDayTypeSyncServerEvents();

    Set<VillagerSensorFactory> villagerSensorFactories();

    Set<ExecutorService> managedExecutors();

    // Exposed for VillagerFishingHook (non-injectable Minecraft entity, server-only)
    FishCatchDataManager fishCatchDataManager();

    // Exposed for SettlementsWolf (non-injectable Minecraft entity, server-only)
    WolfWalkConfig wolfWalkConfig();

    // Exposed for DormantOreBlock (non-injectable Minecraft block, server-only)
    OreRegenDataManager oreRegenDataManager();

    // Exposed for BaseVillager (non-injectable Minecraft entity, server-only)
    VillagerNameDirectory villagerNameDirectory();

    // Exposed for BaseVillager and VillagerTotemItem (non-injectable Minecraft entity and item, server-only)
    SettlementMembershipService settlementMembershipService();

    // Exposed for BaseVillager (non-injectable Minecraft entity, server-only)
    SettlementAssignmentService settlementAssignmentService();

    @Subcomponent.Factory
    interface Factory {
        ServerComponent create();
    }

}
