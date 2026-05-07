package dev.breezes.settlements.infrastructure.network.features.ui.sync.handler;

import dev.breezes.settlements.application.ui.sync.UiClientChannelDefinition;
import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.UiChannel;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.packet.ClientBoundOpenUiPacket;
import dev.breezes.settlements.presentation.ui.sync.UiClientState;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Map;

@CustomLog
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class ClientBoundOpenUiPacketHandler implements ClientSidePacketHandler<ClientBoundOpenUiPacket> {

    private final UiClientState uiClientState;
    private final Map<UiChannel, UiClientChannelDefinition> channelDefinitions;

    @Override
    public void runOnClient(@Nonnull IPayloadContext context, @Nonnull ClientBoundOpenUiPacket packet) {
        UiClientChannelDefinition definition = this.channelDefinitions.get(packet.channel());
        if (definition == null) {
            log.warn("Received open ack for unregistered ui channel {}", packet.channel());
            return;
        }
        this.uiClientState.openSession(packet.channel(), packet.sessionId(), packet.villagerEntityId());
        definition.getScreenOpener().openScreen(packet.sessionId(), packet.villagerEntityId());
    }

}
