package dev.breezes.settlements.infrastructure.network.features.farming.handler;

import dev.breezes.settlements.infrastructure.minecraft.data.farming.crops.CultivationSeedSetClientProjection;
import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.farming.packet.ClientBoundCultivationSeedSetPacket;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class ClientBoundCultivationSeedSetPacketHandler implements ClientSidePacketHandler<ClientBoundCultivationSeedSetPacket> {

    private final CultivationSeedSetClientProjection seedSetClientProjection;

    @Override
    public void runOnClient(@Nonnull IPayloadContext context, @Nonnull ClientBoundCultivationSeedSetPacket packet) {
        this.seedSetClientProjection.applySnapshot(packet.seedItemIds());
    }

}
