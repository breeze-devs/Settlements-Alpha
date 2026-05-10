package dev.breezes.settlements.infrastructure.network.features.ui.stats.handler;

import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerTradeCatalogSnapshotPacket;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import javax.inject.Inject;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class ClientBoundVillagerTradeCatalogSnapshotPacketHandler implements ClientSidePacketHandler<ClientBoundVillagerTradeCatalogSnapshotPacket> {

    private final VillagerStatsSnapshotClientDispatcher dispatcher;

    @Override
    public void runOnClient(@Nonnull IPayloadContext context, @Nonnull ClientBoundVillagerTradeCatalogSnapshotPacket packet) {
        this.dispatcher.apply(packet.sessionId(), "trade catalog snapshot", screen -> screen.applyTradeCatalogSnapshot(packet.snapshot()));
    }

}
