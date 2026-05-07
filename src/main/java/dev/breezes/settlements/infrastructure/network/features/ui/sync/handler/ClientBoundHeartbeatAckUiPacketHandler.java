package dev.breezes.settlements.infrastructure.network.features.ui.sync.handler;

import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.packet.ClientBoundHeartbeatAckUiPacket;
import dev.breezes.settlements.presentation.ui.sync.UiClientState;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import javax.inject.Inject;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class ClientBoundHeartbeatAckUiPacketHandler implements ClientSidePacketHandler<ClientBoundHeartbeatAckUiPacket> {

    private final UiClientState uiClientState;

    @Override
    public void runOnClient(@Nonnull IPayloadContext context, @Nonnull ClientBoundHeartbeatAckUiPacket packet) {
        this.uiClientState.recordHeartbeatAck(packet.sessionId());
    }

}
