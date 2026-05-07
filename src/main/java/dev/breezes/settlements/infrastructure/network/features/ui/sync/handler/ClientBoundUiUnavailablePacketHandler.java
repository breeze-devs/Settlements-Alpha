package dev.breezes.settlements.infrastructure.network.features.ui.sync.handler;

import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.packet.ClientBoundUiUnavailablePacket;
import dev.breezes.settlements.presentation.ui.sync.UiClientState;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import javax.inject.Inject;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class ClientBoundUiUnavailablePacketHandler implements ClientSidePacketHandler<ClientBoundUiUnavailablePacket> {

    private final UiClientState uiClientState;

    @Override
    public void runOnClient(@Nonnull IPayloadContext context, @Nonnull ClientBoundUiUnavailablePacket packet) {
        this.uiClientState.markUnavailable(packet.sessionId(), packet.reasonKey());
    }

}
