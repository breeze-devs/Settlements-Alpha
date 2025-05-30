package dev.breezes.settlements.packet;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

public interface ServerSidePacketHandler<T extends ServerBoundPacket> extends PacketHandler<T> {

    @Override
    default void onReceivePacket(@Nonnull IPayloadContext context, @Nonnull T packet) {
        this.runOnServer(context, packet);
    }

    void runOnServer(@Nonnull IPayloadContext context, @Nonnull T packet);

}
