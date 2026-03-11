package dev.breezes.settlements.infrastructure.network.features.ui.behavior.handler;

import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ClientBoundHeartbeatAckBehaviorControllerPacket;
import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.core.annotations.HandleClientPacket;
import dev.breezes.settlements.presentation.ui.behavior.BehaviorControllerClientState;
import lombok.CustomLog;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

@CustomLog
@HandleClientPacket(ClientBoundHeartbeatAckBehaviorControllerPacket.class)
public class ClientBoundHeartbeatAckBehaviorControllerPacketHandler implements ClientSidePacketHandler<ClientBoundHeartbeatAckBehaviorControllerPacket> {

    @Override
    public void runOnClient(@Nonnull IPayloadContext context, @Nonnull ClientBoundHeartbeatAckBehaviorControllerPacket packet) {
        boolean applied = BehaviorControllerClientState.recordHeartbeatAck(packet.sessionId());
        if (!applied) {
            log.debug("Ignoring stale {} sessionId={}", packet.getClass().getSimpleName(), packet.sessionId());
        }
    }

}
