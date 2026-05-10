package dev.breezes.settlements.di;

import dagger.Subcomponent;
import dev.breezes.settlements.di.modules.client.ClientNetworkModule;
import dev.breezes.settlements.di.modules.client.UiSyncClientModule;
import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketReceiver;
import dev.breezes.settlements.infrastructure.rendering.debug.SettlementDebugOverlayState;
import dev.breezes.settlements.presentation.ui.sync.UiClientState;

@ClientScope
@Subcomponent(modules = {
        ClientNetworkModule.class,
        UiSyncClientModule.class,
})
public interface ClientComponent {

    ClientSidePacketReceiver clientSidePacketReceiver();

    UiClientState uiClientState();

    SettlementDebugOverlayState settlementDebugOverlayState();

    @Subcomponent.Factory
    interface Factory {
        ClientComponent create();
    }

}
