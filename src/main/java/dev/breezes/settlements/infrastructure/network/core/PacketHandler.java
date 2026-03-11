package dev.breezes.settlements.infrastructure.network.core;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

public interface PacketHandler<T extends SettlementsPacket> {

    void onReceivePacket(@Nonnull IPayloadContext context, @Nonnull T packet);

}
