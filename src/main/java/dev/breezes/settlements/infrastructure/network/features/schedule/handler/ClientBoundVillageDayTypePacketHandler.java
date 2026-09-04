package dev.breezes.settlements.infrastructure.network.features.schedule.handler;

import dev.breezes.settlements.infrastructure.minecraft.data.schedule.VillageDayTypeClientProjection;
import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.schedule.packet.ClientBoundVillageDayTypePacket;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class ClientBoundVillageDayTypePacketHandler implements ClientSidePacketHandler<ClientBoundVillageDayTypePacket> {

    private final VillageDayTypeClientProjection dayTypeClientProjection;

    @Override
    public void runOnClient(@Nonnull IPayloadContext context, @Nonnull ClientBoundVillageDayTypePacket packet) {
        this.dayTypeClientProjection.applySnapshot(packet.calendarDay(), packet.dayType());
    }

}
