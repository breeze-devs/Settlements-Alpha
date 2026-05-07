package dev.breezes.settlements.di.modules.server;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.ClassKey;
import dagger.multibindings.IntoMap;
import dev.breezes.settlements.infrastructure.network.core.ServerSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.handler.ServerBoundCloseBehaviorControllerPacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.handler.ServerBoundHeartbeatBehaviorControllerPacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.handler.ServerBoundOpenBehaviorControllerPacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ServerBoundCloseBehaviorControllerPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ServerBoundHeartbeatBehaviorControllerPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ServerBoundOpenBehaviorControllerPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.handler.ServerBoundCloseVillagerStatsPacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.handler.ServerBoundHeartbeatVillagerStatsPacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.handler.ServerBoundOpenVillagerStatsPacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ServerBoundCloseVillagerStatsPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ServerBoundHeartbeatVillagerStatsPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ServerBoundOpenVillagerStatsPacket;
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

    @Binds
    @IntoMap
    @ClassKey(ServerBoundOpenBehaviorControllerPacket.class)
    abstract ServerSidePacketHandler<?> openBehavior(ServerBoundOpenBehaviorControllerPacketHandler impl);

    @Binds
    @IntoMap
    @ClassKey(ServerBoundCloseBehaviorControllerPacket.class)
    abstract ServerSidePacketHandler<?> closeBehavior(ServerBoundCloseBehaviorControllerPacketHandler impl);

    @Binds
    @IntoMap
    @ClassKey(ServerBoundHeartbeatBehaviorControllerPacket.class)
    abstract ServerSidePacketHandler<?> heartbeatBehavior(ServerBoundHeartbeatBehaviorControllerPacketHandler impl);

    @Binds
    @IntoMap
    @ClassKey(ServerBoundOpenVillagerStatsPacket.class)
    abstract ServerSidePacketHandler<?> openStats(ServerBoundOpenVillagerStatsPacketHandler impl);

    @Binds
    @IntoMap
    @ClassKey(ServerBoundCloseVillagerStatsPacket.class)
    abstract ServerSidePacketHandler<?> closeStats(ServerBoundCloseVillagerStatsPacketHandler impl);

    @Binds
    @IntoMap
    @ClassKey(ServerBoundHeartbeatVillagerStatsPacket.class)
    abstract ServerSidePacketHandler<?> heartbeatStats(ServerBoundHeartbeatVillagerStatsPacketHandler impl);

}
