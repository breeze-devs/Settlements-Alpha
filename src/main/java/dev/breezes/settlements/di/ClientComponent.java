package dev.breezes.settlements.di;

import dagger.Subcomponent;
import dev.breezes.settlements.di.modules.client.ClientNetworkModule;
import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketReceiver;
import dev.breezes.settlements.presentation.ui.behavior.BehaviorControllerClientState;
import dev.breezes.settlements.presentation.ui.stats.VillagerStatsClientState;

@ClientScope
@Subcomponent(modules = {
        ClientNetworkModule.class,
})
public interface ClientComponent {

    ClientSidePacketReceiver clientSidePacketReceiver();

    BehaviorControllerClientState behaviorControllerClientState();

    VillagerStatsClientState villagerStatsClientState();

    @Subcomponent.Factory
    interface Factory {
        ClientComponent create();
    }

}
