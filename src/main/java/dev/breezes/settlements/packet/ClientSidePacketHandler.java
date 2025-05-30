package dev.breezes.settlements.packet;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

public interface ClientSidePacketHandler<T extends ClientBoundPacket> extends PacketHandler<T> {

    @Override
    default void onReceivePacket(@Nonnull IPayloadContext context, @Nonnull T packet) {
        this.runOnClient(context, packet);
    }

    void runOnClient(@Nonnull IPayloadContext context, @Nonnull T packet);

}
