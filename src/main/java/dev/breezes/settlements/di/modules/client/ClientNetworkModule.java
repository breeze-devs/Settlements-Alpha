package dev.breezes.settlements.di.modules.client;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.ClassKey;
import dagger.multibindings.IntoMap;
import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.debug.handler.ClientBoundSettlementDebugPacketHandler;
import dev.breezes.settlements.infrastructure.network.features.debug.packet.ClientBoundSettlementDebugPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.bubble.handler.ClientBoundBubbleSnapshotPacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.bubble.packet.ClientBoundBubbleSnapshotPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.dayplan.handler.ClientBoundDayPlanSnapshotPacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.dayplan.packet.ClientBoundDayPlanSnapshotPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.handler.ClientBoundVillagerDemandSnapshotPacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.handler.ClientBoundVillagerInventorySnapshotPacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.handler.ClientBoundVillagerStatsSnapshotPacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.handler.ClientBoundVillagerTradeCatalogSnapshotPacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerDemandSnapshotPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerInventorySnapshotPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerStatsSnapshotPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerTradeCatalogSnapshotPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.handler.ClientBoundHeartbeatAckUiPacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.handler.ClientBoundOpenUiPacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.handler.ClientBoundUiUnavailablePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.packet.ClientBoundHeartbeatAckUiPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.packet.ClientBoundOpenUiPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.packet.ClientBoundUiUnavailablePacket;

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
    @ClassKey(ClientBoundOpenUiPacket.class)
    abstract ClientSidePacketHandler<?> openUi(ClientBoundOpenUiPacketHandler impl);

    @Binds
    @IntoMap
    @ClassKey(ClientBoundHeartbeatAckUiPacket.class)
    abstract ClientSidePacketHandler<?> heartbeatAckUi(ClientBoundHeartbeatAckUiPacketHandler impl);

    @Binds
    @IntoMap
    @ClassKey(ClientBoundUiUnavailablePacket.class)
    abstract ClientSidePacketHandler<?> unavailableUi(ClientBoundUiUnavailablePacketHandler impl);

    @Binds
    @IntoMap
    @ClassKey(ClientBoundDayPlanSnapshotPacket.class)
    abstract ClientSidePacketHandler<?> dayPlanSnapshot(ClientBoundDayPlanSnapshotPacketHandler impl);

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

}
