package dev.breezes.settlements.infrastructure.network.features.ui.sync.handler;

import dev.breezes.settlements.application.ui.sync.session.UiSessionRegistry;
import dev.breezes.settlements.infrastructure.network.core.ServerSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.packet.ServerBoundCloseUiPacket;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.UUID;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class ServerBoundCloseUiPacketHandler implements ServerSidePacketHandler<ServerBoundCloseUiPacket> {

    private final UiSessionRegistry sessionRegistry;

    @Override
    public void runOnServer(@Nonnull IPayloadContext context, @Nonnull ServerBoundCloseUiPacket packet) {
        UUID playerUuid = context.player().getUUID();
        if (this.sessionRegistry.isSessionStale(playerUuid, packet.sessionId())) {
            return;
        }
        this.sessionRegistry.closeSession(playerUuid, packet.sessionId());
    }

}
