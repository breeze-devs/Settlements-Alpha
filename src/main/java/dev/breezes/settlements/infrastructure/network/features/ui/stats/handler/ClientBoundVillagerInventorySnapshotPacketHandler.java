package dev.breezes.settlements.infrastructure.network.features.ui.stats.handler;

import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerInventorySnapshotPacket;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class ClientBoundVillagerInventorySnapshotPacketHandler implements ClientSidePacketHandler<ClientBoundVillagerInventorySnapshotPacket> {

    private final VillagerStatsSnapshotClientDispatcher dispatcher;

    @Override
    public void runOnClient(@Nonnull IPayloadContext context, @Nonnull ClientBoundVillagerInventorySnapshotPacket packet) {
        this.dispatcher.apply(packet.sessionId(), "inventory snapshot", screen -> screen.applyInventorySnapshot(packet.inventory()));
    }

}
