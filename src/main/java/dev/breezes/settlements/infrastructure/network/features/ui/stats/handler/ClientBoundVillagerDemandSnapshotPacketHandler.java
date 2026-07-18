package dev.breezes.settlements.infrastructure.network.features.ui.stats.handler;

import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerDemandSnapshotPacket;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class ClientBoundVillagerDemandSnapshotPacketHandler implements ClientSidePacketHandler<ClientBoundVillagerDemandSnapshotPacket> {

    private final VillagerStatsSnapshotClientDispatcher dispatcher;

    @Override
    public void runOnClient(@Nonnull IPayloadContext context, @Nonnull ClientBoundVillagerDemandSnapshotPacket packet) {
        this.dispatcher.apply(packet.sessionId(), "demand snapshot", screen -> screen.applyDemandSnapshot(packet.snapshot()));
    }

}
