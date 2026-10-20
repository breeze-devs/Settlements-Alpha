package dev.breezes.settlements.infrastructure.network.features.arrival.handler;

import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.arrival.packet.ClientBoundSettlementArrivalPacket;
import dev.breezes.settlements.presentation.ui.hud.SettlementAnnouncementQueue;
import dev.breezes.settlements.shared.util.ClientMonotonicClock;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class ClientBoundSettlementArrivalPacketHandler implements ClientSidePacketHandler<ClientBoundSettlementArrivalPacket> {

    private final SettlementAnnouncementQueue announcementQueue;

    @Override
    public void runOnClient(@Nonnull IPayloadContext context, @Nonnull ClientBoundSettlementArrivalPacket packet) {
        this.announcementQueue.offer(packet, ClientMonotonicClock.nowMillis());
    }

}
