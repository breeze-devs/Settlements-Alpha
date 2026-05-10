package dev.breezes.settlements.di.modules.server;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.ClassKey;
import dagger.multibindings.IntoMap;
import dev.breezes.settlements.infrastructure.network.core.ServerSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.handler.ServerBoundCloseUiPacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.handler.ServerBoundHeartbeatUiPacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.handler.ServerBoundOpenUiPacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.packet.ServerBoundCloseUiPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.packet.ServerBoundHeartbeatUiPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.packet.ServerBoundOpenUiPacket;

@Module
public abstract class ServerNetworkModule {

    @Binds
    @IntoMap
    @ClassKey(ServerBoundOpenUiPacket.class)
    abstract ServerSidePacketHandler<?> openUi(ServerBoundOpenUiPacketHandler impl);

    @Binds
    @IntoMap
    @ClassKey(ServerBoundCloseUiPacket.class)
    abstract ServerSidePacketHandler<?> closeUi(ServerBoundCloseUiPacketHandler impl);

    @Binds
    @IntoMap
    @ClassKey(ServerBoundHeartbeatUiPacket.class)
    abstract ServerSidePacketHandler<?> heartbeatUi(ServerBoundHeartbeatUiPacketHandler impl);

}
