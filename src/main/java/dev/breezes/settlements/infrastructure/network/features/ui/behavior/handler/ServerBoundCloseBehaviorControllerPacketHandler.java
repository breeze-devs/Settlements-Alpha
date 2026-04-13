package dev.breezes.settlements.infrastructure.network.features.ui.behavior.handler;

import dev.breezes.settlements.application.ui.behavior.session.BehaviorControllerSession;
import dev.breezes.settlements.application.ui.behavior.session.BehaviorControllerSessionService;
import dev.breezes.settlements.infrastructure.network.core.ServerSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ServerBoundCloseBehaviorControllerPacket;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import javax.inject.Inject;

@CustomLog
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class ServerBoundCloseBehaviorControllerPacketHandler implements ServerSidePacketHandler<ServerBoundCloseBehaviorControllerPacket> {

    private final BehaviorControllerSessionService sessionService;

    @Override
    public void runOnServer(@Nonnull IPayloadContext context, @Nonnull ServerBoundCloseBehaviorControllerPacket packet) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        boolean staleCloseRequest = sessionService.isSessionStale(player.getUUID(), packet.sessionId());

        if (staleCloseRequest) {
            log.debug("Ignoring close for stale behavior controller sessionId={} player={} activeSessionId={}",
                    packet.sessionId(), player.getUUID(),
                    sessionService.getSession(player.getUUID()).map(BehaviorControllerSession::getSessionId).orElse(-1L));
            return;
        }

        sessionService.closeSession(player.getUUID(), packet.sessionId());

        log.debug("Closed behavior controller sessionId={} for player={}", packet.sessionId(), player.getUUID());
    }

}
