package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.di.ClientComponent;
import dev.breezes.settlements.di.ClientSessionComponent;
import dev.breezes.settlements.di.ClientSessionResettable;
import dev.breezes.settlements.di.SettlementsDagger;
import lombok.CustomLog;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@CustomLog
@EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ClientSessionLifecycleEvents {

    @SubscribeEvent
    public static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        log.info("Initializing client session subcomponent");
        ClientSessionComponent clientSessionComponent = SettlementsDagger.client()
                .clientSessionComponentFactory()
                .create();
        SettlementsDagger.initializeClientSession(clientSessionComponent);
    }

    @SubscribeEvent
    public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        log.info("Clearing client session subcomponent");
        resetClientScopedSessionState();
        SettlementsDagger.clearClientSession();
    }

    /**
     * Discarding the session subcomponent only reclaims what was built inside it. Session state held by
     * client-scope singletons outlives the process's every world, so it is reset here or not at all.
     */
    private static void resetClientScopedSessionState() {
        // Defensive accessor: logging out is also reached while the client is tearing down, where the graph
        // may already be gone and a throwing lookup would turn a clean exit into a crash.
        ClientComponent clientComponent = SettlementsDagger.clientOrNull();
        if (clientComponent == null) {
            return;
        }

        clientComponent.clientSessionResettables().forEach(ClientSessionResettable::onClientSessionEnded);
    }

}
