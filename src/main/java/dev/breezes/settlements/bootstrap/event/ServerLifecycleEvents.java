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

@EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
@CustomLog
public final class ServerLifecycleEvents {

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        log.info("Initializing server subcomponent");
        ServerComponent serverComponent = SettlementsDagger.component().serverComponentFactory().create();
        SettlementsDagger.initializeServer(serverComponent);

        log.info("Registering events");
        NeoForge.EVENT_BUS.register(serverComponent.behaviorControllerServerEvents());
        NeoForge.EVENT_BUS.register(serverComponent.villagerStatsServerEvents());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        log.info("De-registering events");
        ServerComponent serverComponent = SettlementsDagger.serverOrNull();
        if (serverComponent != null) {
            // Because they are @ServerScoped, Dagger returns the exact instances we registered earlier
            NeoForge.EVENT_BUS.unregister(serverComponent.behaviorControllerServerEvents());
            NeoForge.EVENT_BUS.unregister(serverComponent.villagerStatsServerEvents());
        }

        log.info("Clearing server subcomponent");
        SettlementsDagger.clearServer();
    }

}
