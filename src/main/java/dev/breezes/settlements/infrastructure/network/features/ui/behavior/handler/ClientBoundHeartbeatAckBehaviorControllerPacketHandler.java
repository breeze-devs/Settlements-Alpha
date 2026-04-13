package dev.breezes.settlements.infrastructure.network.features.ui.behavior.handler;

import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ClientBoundHeartbeatAckBehaviorControllerPacket;
import dev.breezes.settlements.presentation.ui.behavior.BehaviorControllerClientState;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import javax.inject.Inject;

@CustomLog
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class ClientBoundHeartbeatAckBehaviorControllerPacketHandler implements ClientSidePacketHandler<ClientBoundHeartbeatAckBehaviorControllerPacket> {

    private final BehaviorControllerClientState behaviorControllerClientState;

    @Override
    public void runOnClient(@Nonnull IPayloadContext context, @Nonnull ClientBoundHeartbeatAckBehaviorControllerPacket packet) {
        boolean applied = this.behaviorControllerClientState.recordHeartbeatAck(packet.sessionId());
        if (!applied) {
            log.debug("Ignoring stale {} sessionId={}", packet.getClass().getSimpleName(), packet.sessionId());
        }
    }

}
