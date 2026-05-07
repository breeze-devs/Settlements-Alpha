package dev.breezes.settlements.infrastructure.network.features.ui.sync.handler;

import dev.breezes.settlements.application.ui.sync.session.UiSessionRegistry;
import dev.breezes.settlements.infrastructure.network.core.ServerSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.packet.ClientBoundHeartbeatAckUiPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.packet.ServerBoundHeartbeatUiPacket;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.UUID;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class ServerBoundHeartbeatUiPacketHandler implements ServerSidePacketHandler<ServerBoundHeartbeatUiPacket> {

    private final UiSessionRegistry sessionRegistry;

    @Override
    public void runOnServer(@Nonnull IPayloadContext context, @Nonnull ServerBoundHeartbeatUiPacket packet) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        UUID playerUuid = player.getUUID();
        if (this.sessionRegistry.isSessionStale(playerUuid, packet.sessionId())) {
            return;
        }
        long gameTime = player.serverLevel().getGameTime();
        this.sessionRegistry.recordHeartbeat(playerUuid, packet.sessionId(), gameTime);
        PacketDistributor.sendToPlayer(player, new ClientBoundHeartbeatAckUiPacket(packet.channel(), packet.sessionId()));
    }

}
