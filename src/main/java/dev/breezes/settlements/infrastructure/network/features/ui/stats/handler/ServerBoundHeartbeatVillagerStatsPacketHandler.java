package dev.breezes.settlements.infrastructure.network.features.ui.stats.handler;

import dev.breezes.settlements.application.ui.stats.session.VillagerStatsSession;
import dev.breezes.settlements.application.ui.stats.session.VillagerStatsSessionService;
import dev.breezes.settlements.infrastructure.network.core.ServerSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundHeartbeatAckVillagerStatsPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ServerBoundHeartbeatVillagerStatsPacket;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import javax.inject.Inject;

@CustomLog
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class ServerBoundHeartbeatVillagerStatsPacketHandler implements ServerSidePacketHandler<ServerBoundHeartbeatVillagerStatsPacket> {

    private final VillagerStatsSessionService sessionService;

    @Override
    public void runOnServer(@Nonnull IPayloadContext context, @Nonnull ServerBoundHeartbeatVillagerStatsPacket packet) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        log.debug("Heartbeat received for villager stats sessionId={} player={}", packet.sessionId(), player.getUUID());

        long gameTime = player.serverLevel().getGameTime();
        boolean staleHeartbeat = sessionService.isSessionStale(player.getUUID(), packet.sessionId());

        if (staleHeartbeat) {
            log.debug("Ignoring stale heartbeat sessionId={} for player={} activeSessionId={}",
                    packet.sessionId(), player.getUUID(),
                    sessionService.getSession(player.getUUID()).map(VillagerStatsSession::getSessionId).orElse(-1L));
            return;
        }

        sessionService.recordHeartbeat(player.getUUID(), packet.sessionId(), gameTime);
        PacketDistributor.sendToPlayer(player, new ClientBoundHeartbeatAckVillagerStatsPacket(packet.sessionId()));
    }

}
