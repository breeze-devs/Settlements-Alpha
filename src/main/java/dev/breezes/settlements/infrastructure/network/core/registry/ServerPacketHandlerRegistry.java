package dev.breezes.settlements.infrastructure.network.core.registry;

import dev.breezes.settlements.infrastructure.network.core.ServerBoundPacket;
import dev.breezes.settlements.infrastructure.network.core.ServerSidePacketHandler;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class ServerPacketHandlerRegistry {

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
    private static Map<Class<? extends ServerBoundPacket>, ServerSidePacketHandler<? extends ServerBoundPacket>> registeredPacketHandlers = Map.of();

    public static void initialize(@Nonnull Map<Class<? extends ServerBoundPacket>, ServerSidePacketHandler<? extends ServerBoundPacket>> map) {
        if (!INITIALIZED.compareAndSet(false, true)) {
            throw new IllegalStateException("ServerPacketHandlerRegistry already initialized");
        }

        registeredPacketHandlers = Map.copyOf(map);
    }

    public static Optional<ServerSidePacketHandler<? extends ServerBoundPacket>> get(@Nonnull Class<? extends ServerBoundPacket> packetClass) {
        return Optional.ofNullable(registeredPacketHandlers.get(packetClass));
    }

    public static boolean contains(@Nonnull Class<? extends ServerBoundPacket> packetClass) {
        return registeredPacketHandlers.containsKey(packetClass);
    }

}
