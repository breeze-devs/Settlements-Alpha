package dev.breezes.settlements.infrastructure.network.features.debug.handler;

import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.debug.packet.ClientBoundSettlementDebugPacket;
import dev.breezes.settlements.infrastructure.rendering.debug.SettlementDebugOverlayState;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import javax.inject.Inject;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class ClientBoundSettlementDebugPacketHandler implements ClientSidePacketHandler<ClientBoundSettlementDebugPacket> {

    private final SettlementDebugOverlayState settlementDebugOverlayState;

    @Override
    public void runOnClient(@Nonnull IPayloadContext context, @Nonnull ClientBoundSettlementDebugPacket packet) {
        this.settlementDebugOverlayState.set(packet);
    }

}
