package dev.breezes.settlements.di.modules.client;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.ClassKey;
import dagger.multibindings.IntoMap;
import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.debug.handler.ClientBoundSettlementDebugPacketHandler;
import dev.breezes.settlements.infrastructure.network.features.debug.packet.ClientBoundSettlementDebugPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.handler.ClientBoundBehaviorControllerSnapshotPacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.handler.ClientBoundBehaviorControllerUnavailablePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.handler.ClientBoundHeartbeatAckBehaviorControllerPacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.handler.ClientBoundOpenBehaviorControllerPacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ClientBoundBehaviorControllerSnapshotPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ClientBoundBehaviorControllerUnavailablePacket;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ClientBoundHeartbeatAckBehaviorControllerPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ClientBoundOpenBehaviorControllerPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.bubble.handler.ClientBoundBubbleSnapshotPacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.bubble.packet.ClientBoundBubbleSnapshotPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.handler.ClientBoundHeartbeatAckVillagerStatsPacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.handler.ClientBoundOpenVillagerStatsPacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.handler.ClientBoundVillagerDemandSnapshotPacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.handler.ClientBoundVillagerInventorySnapshotPacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.handler.ClientBoundVillagerStatsSnapshotPacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.handler.ClientBoundVillagerStatsUnavailablePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.handler.ClientBoundVillagerTradeCatalogSnapshotPacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundHeartbeatAckVillagerStatsPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundOpenVillagerStatsPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerDemandSnapshotPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerInventorySnapshotPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerStatsSnapshotPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerStatsUnavailablePacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerTradeCatalogSnapshotPacket;

@Module
public abstract class ClientNetworkModule {

    @Binds
    @IntoMap
    @ClassKey(ClientBoundSettlementDebugPacket.class)
    abstract ClientSidePacketHandler<?> settlementDebug(ClientBoundSettlementDebugPacketHandler impl);

    @Binds
    @IntoMap
    @ClassKey(ClientBoundBubbleSnapshotPacket.class)
    abstract ClientSidePacketHandler<?> bubbleSnapshot(ClientBoundBubbleSnapshotPacketHandler impl);

    @Binds
    @IntoMap
    @ClassKey(ClientBoundBehaviorControllerSnapshotPacket.class)
    abstract ClientSidePacketHandler<?> behaviorSnapshot(ClientBoundBehaviorControllerSnapshotPacketHandler impl);

    @Binds
    @IntoMap
    @ClassKey(ClientBoundBehaviorControllerUnavailablePacket.class)
    abstract ClientSidePacketHandler<?> behaviorUnavailable(ClientBoundBehaviorControllerUnavailablePacketHandler impl);

    @Binds
    @IntoMap
    @ClassKey(ClientBoundHeartbeatAckBehaviorControllerPacket.class)
    abstract ClientSidePacketHandler<?> behaviorHeartbeatAck(ClientBoundHeartbeatAckBehaviorControllerPacketHandler impl);

    @Binds
    @IntoMap
    @ClassKey(ClientBoundOpenBehaviorControllerPacket.class)
    abstract ClientSidePacketHandler<?> behaviorOpen(ClientBoundOpenBehaviorControllerPacketHandler impl);

    @Binds
    @IntoMap
    @ClassKey(ClientBoundVillagerStatsSnapshotPacket.class)
    abstract ClientSidePacketHandler<?> statsSnapshot(ClientBoundVillagerStatsSnapshotPacketHandler impl);

    @Binds
    @IntoMap
    @ClassKey(ClientBoundVillagerInventorySnapshotPacket.class)
    abstract ClientSidePacketHandler<?> inventorySnapshot(ClientBoundVillagerInventorySnapshotPacketHandler impl);

    @Binds
    @IntoMap
    @ClassKey(ClientBoundVillagerTradeCatalogSnapshotPacket.class)
    abstract ClientSidePacketHandler<?> tradeCatalogSnapshot(ClientBoundVillagerTradeCatalogSnapshotPacketHandler impl);

    @Binds
    @IntoMap
    @ClassKey(ClientBoundVillagerDemandSnapshotPacket.class)
    abstract ClientSidePacketHandler<?> demandSnapshot(ClientBoundVillagerDemandSnapshotPacketHandler impl);

    @Binds
    @IntoMap
    @ClassKey(ClientBoundVillagerStatsUnavailablePacket.class)
    abstract ClientSidePacketHandler<?> statsUnavailable(ClientBoundVillagerStatsUnavailablePacketHandler impl);

    @Binds
    @IntoMap
    @ClassKey(ClientBoundHeartbeatAckVillagerStatsPacket.class)
    abstract ClientSidePacketHandler<?> statsHeartbeatAck(ClientBoundHeartbeatAckVillagerStatsPacketHandler impl);

    @Binds
    @IntoMap
    @ClassKey(ClientBoundOpenVillagerStatsPacket.class)
    abstract ClientSidePacketHandler<?> statsOpen(ClientBoundOpenVillagerStatsPacketHandler impl);

}
