package dev.breezes.settlements.infrastructure.network.features.ui.stats.handler;

import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundHeartbeatAckVillagerStatsPacket;
import dev.breezes.settlements.presentation.ui.stats.VillagerStatsClientState;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import javax.inject.Inject;

@CustomLog
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class ClientBoundHeartbeatAckVillagerStatsPacketHandler implements ClientSidePacketHandler<ClientBoundHeartbeatAckVillagerStatsPacket> {

    private final VillagerStatsClientState villagerStatsClientState;

    @Override
    public void runOnClient(@Nonnull IPayloadContext context, @Nonnull ClientBoundHeartbeatAckVillagerStatsPacket packet) {
        boolean applied = this.villagerStatsClientState.recordHeartbeatAck(packet.sessionId());
        if (!applied) {
            log.debug("Ignoring stale {} sessionId={}", packet.getClass().getSimpleName(), packet.sessionId());
        }
    }

}
