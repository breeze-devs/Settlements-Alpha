package dev.breezes.settlements.infrastructure.network.features.flare.handler;

import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.flare.packet.ClientBoundFlarePacket;
import dev.breezes.settlements.infrastructure.rendering.flare.FlarePlayback;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class ClientBoundFlarePacketHandler implements ClientSidePacketHandler<ClientBoundFlarePacket> {

    private final FlarePlayback flarePlayback;

    @Override
    public void runOnClient(@Nonnull IPayloadContext context, @Nonnull ClientBoundFlarePacket packet) {
        this.flarePlayback.launch(packet.hideWithin(), packet.flares());
    }

}
