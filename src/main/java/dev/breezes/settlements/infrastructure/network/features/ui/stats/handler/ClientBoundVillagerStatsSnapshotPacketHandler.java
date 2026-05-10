package dev.breezes.settlements.infrastructure.network.features.ui.stats.handler;

import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerStatsSnapshotPacket;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import javax.inject.Inject;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class ClientBoundVillagerStatsSnapshotPacketHandler implements ClientSidePacketHandler<ClientBoundVillagerStatsSnapshotPacket> {

    private final VillagerStatsSnapshotClientDispatcher dispatcher;

    @Override
    public void runOnClient(@Nonnull IPayloadContext context, @Nonnull ClientBoundVillagerStatsSnapshotPacket packet) {
        this.dispatcher.apply(packet.sessionId(), "stats snapshot", screen -> screen.applyStatsSnapshot(packet.snapshot()));
    }

}
