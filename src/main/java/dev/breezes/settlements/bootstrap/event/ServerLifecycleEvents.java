package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.SettlementsMod;
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
        NeoForge.EVENT_BUS.register(serverComponent.gossipSessionReaperServerEvents());
        NeoForge.EVENT_BUS.register(serverComponent.credibilityDecayServerEvents());
        NeoForge.EVENT_BUS.register(serverComponent.eveningDialoguePackSweepServerEvents());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        log.info("De-registering events");
        ServerComponent serverComponent = SettlementsDagger.serverOrNull();
        if (serverComponent != null) {
            shutdownPlanGenerationExecutor(serverComponent.planGenerationExecutor());

            // Because they are @ServerScoped, Dagger returns the exact instances we registered earlier
            NeoForge.EVENT_BUS.unregister(serverComponent.playerSettlementTracker());
            NeoForge.EVENT_BUS.unregister(serverComponent.regionSubtitleHandler());
            NeoForge.EVENT_BUS.unregister(serverComponent.settlementMetadataPersistenceServerEvents());
            NeoForge.EVENT_BUS.unregister(serverComponent.uiSyncServerEvents());
            NeoForge.EVENT_BUS.unregister(serverComponent.courtshipSessionReaperServerEvents());
            NeoForge.EVENT_BUS.unregister(serverComponent.worldEventBusReaperServerEvents());
            NeoForge.EVENT_BUS.unregister(serverComponent.gossipSessionReaperServerEvents());
            NeoForge.EVENT_BUS.unregister(serverComponent.credibilityDecayServerEvents());
            NeoForge.EVENT_BUS.unregister(serverComponent.eveningDialoguePackSweepServerEvents());
        }

        log.info("Clearing server subcomponent");
        SettlementsDagger.clearServer();
    }

    private static void shutdownPlanGenerationExecutor(ExecutorService executor) {
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

}
