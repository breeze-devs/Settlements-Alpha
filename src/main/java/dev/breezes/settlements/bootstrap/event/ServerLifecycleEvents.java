package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.application.ai.inference.InferenceTransport;
import dev.breezes.settlements.di.ServerComponent;
import dev.breezes.settlements.di.SettlementsDagger;
import lombok.CustomLog;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
@CustomLog
public final class ServerLifecycleEvents {

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        log.info("Initializing server subcomponent");
        ServerComponent serverComponent = SettlementsDagger.component().serverComponentFactory().create();
        SettlementsDagger.initializeServer(serverComponent);

        log.info("Registering events");
        NeoForge.EVENT_BUS.register(serverComponent.playerSettlementTracker());
        NeoForge.EVENT_BUS.register(serverComponent.regionSubtitleHandler());
        NeoForge.EVENT_BUS.register(serverComponent.settlementMetadataPersistenceServerEvents());
        NeoForge.EVENT_BUS.register(serverComponent.uiSyncServerEvents());
        NeoForge.EVENT_BUS.register(serverComponent.courtshipSessionReaperServerEvents());
        NeoForge.EVENT_BUS.register(serverComponent.worldEventBusReaperServerEvents());
        NeoForge.EVENT_BUS.register(serverComponent.resourceIndexRefresherServerEvents());
        NeoForge.EVENT_BUS.register(serverComponent.gossipSessionReaperServerEvents());
        NeoForge.EVENT_BUS.register(serverComponent.tradeSessionReaperServerEvents());
        NeoForge.EVENT_BUS.register(serverComponent.credibilityDecayServerEvents());
        NeoForge.EVENT_BUS.register(serverComponent.eveningDialoguePackSweepServerEvents());
        NeoForge.EVENT_BUS.register(serverComponent.villagerZombificationServerEvents());
        NeoForge.EVENT_BUS.register(serverComponent.villageAnimalSpawnerServerEvents());
        NeoForge.EVENT_BUS.register(serverComponent.worldgenVillagerReplacementServerEvents());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        log.info("De-registering events");
        ServerComponent serverComponent = SettlementsDagger.serverOrNull();
        if (serverComponent != null) {
            serverComponent.managedExecutors().forEach(ServerLifecycleEvents::shutdownExecutor);
            closeInferenceTransport(serverComponent.inferenceTransport());

            // Because they are @ServerScoped, Dagger returns the exact instances we registered earlier
            NeoForge.EVENT_BUS.unregister(serverComponent.playerSettlementTracker());
            NeoForge.EVENT_BUS.unregister(serverComponent.regionSubtitleHandler());
            NeoForge.EVENT_BUS.unregister(serverComponent.settlementMetadataPersistenceServerEvents());
            NeoForge.EVENT_BUS.unregister(serverComponent.uiSyncServerEvents());
            NeoForge.EVENT_BUS.unregister(serverComponent.courtshipSessionReaperServerEvents());
            NeoForge.EVENT_BUS.unregister(serverComponent.worldEventBusReaperServerEvents());
            NeoForge.EVENT_BUS.unregister(serverComponent.resourceIndexRefresherServerEvents());
            NeoForge.EVENT_BUS.unregister(serverComponent.gossipSessionReaperServerEvents());
            NeoForge.EVENT_BUS.unregister(serverComponent.tradeSessionReaperServerEvents());
            NeoForge.EVENT_BUS.unregister(serverComponent.credibilityDecayServerEvents());
            NeoForge.EVENT_BUS.unregister(serverComponent.eveningDialoguePackSweepServerEvents());
            NeoForge.EVENT_BUS.unregister(serverComponent.villagerZombificationServerEvents());
            NeoForge.EVENT_BUS.unregister(serverComponent.villageAnimalSpawnerServerEvents());
            NeoForge.EVENT_BUS.unregister(serverComponent.worldgenVillagerReplacementServerEvents());
        }

        log.info("Clearing server subcomponent");
        SettlementsDagger.clearServer();
    }

    private static void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static void closeInferenceTransport(InferenceTransport transport) {
        try {
            transport.close();
        } catch (RuntimeException exception) {
            log.error("Failed to close inference transport", exception);
        }
    }

}
