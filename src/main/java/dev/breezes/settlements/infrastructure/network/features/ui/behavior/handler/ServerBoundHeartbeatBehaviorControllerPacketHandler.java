package dev.breezes.settlements.infrastructure.network.features.ui.behavior.handler;

import dev.breezes.settlements.application.ui.behavior.session.BehaviorControllerSession;
import dev.breezes.settlements.application.ui.behavior.session.BehaviorControllerSessionService;
import dev.breezes.settlements.infrastructure.network.core.ServerSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.core.annotations.HandleServerPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ClientBoundHeartbeatAckBehaviorControllerPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ServerBoundHeartbeatBehaviorControllerPacket;
import lombok.CustomLog;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

@CustomLog
@HandleServerPacket(ServerBoundHeartbeatBehaviorControllerPacket.class)
public class ServerBoundHeartbeatBehaviorControllerPacketHandler implements ServerSidePacketHandler<ServerBoundHeartbeatBehaviorControllerPacket> {
    @Override
    public void runOnServer(@Nonnull IPayloadContext context, @Nonnull ServerBoundHeartbeatBehaviorControllerPacket packet) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        log.debug("Heartbeat received for behavior controller sessionId={} player={}", packet.sessionId(), player.getUUID());

        long gameTime = player.serverLevel().getGameTime();
        BehaviorControllerSessionService service = BehaviorControllerSessionService.getInstance();
        boolean staleHeartbeat = service.isSessionStale(player.getUUID(), packet.sessionId());

        if (staleHeartbeat) {
            log.debug("Ignoring stale heartbeat sessionId={} for player={} activeSessionId={}",
                    packet.sessionId(), player.getUUID(),
                    service.getSession(player.getUUID()).map(BehaviorControllerSession::getSessionId).orElse(-1L));
            return;
        }

        service.recordHeartbeat(player.getUUID(), packet.sessionId(), gameTime);
        PacketDistributor.sendToPlayer(player, new ClientBoundHeartbeatAckBehaviorControllerPacket(packet.sessionId()));
    }

}
