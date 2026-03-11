package dev.breezes.settlements.infrastructure.network.features.ui.behavior.handler;

import dev.breezes.settlements.application.ui.behavior.session.BehaviorControllerSession;
import dev.breezes.settlements.application.ui.behavior.session.BehaviorControllerSessionService;
import dev.breezes.settlements.infrastructure.network.core.ServerSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.core.annotations.HandleServerPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ServerBoundCloseBehaviorControllerPacket;
import lombok.CustomLog;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

@CustomLog
@HandleServerPacket(ServerBoundCloseBehaviorControllerPacket.class)
public class ServerBoundCloseBehaviorControllerPacketHandler implements ServerSidePacketHandler<ServerBoundCloseBehaviorControllerPacket> {

    @Override
    public void runOnServer(@Nonnull IPayloadContext context, @Nonnull ServerBoundCloseBehaviorControllerPacket packet) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        BehaviorControllerSessionService sessions = BehaviorControllerSessionService.getInstance();
        boolean staleCloseRequest = sessions.isSessionStale(player.getUUID(), packet.sessionId());

        if (staleCloseRequest) {
            log.debug("Ignoring close for stale behavior controller sessionId={} player={} activeSessionId={}",
                    packet.sessionId(), player.getUUID(),
                    sessions.getSession(player.getUUID()).map(BehaviorControllerSession::getSessionId).orElse(-1L));
            return;
        }

        sessions.closeSession(player.getUUID(), packet.sessionId());

        log.debug("Closed behavior controller sessionId={} for player={}", packet.sessionId(), player.getUUID());
    }

}
