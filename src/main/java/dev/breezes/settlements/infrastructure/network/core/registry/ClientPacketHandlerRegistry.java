package dev.breezes.settlements.infrastructure.network.core.registry;

import dev.breezes.settlements.infrastructure.network.core.ClientBoundPacket;
import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketHandler;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class ClientPacketHandlerRegistry {

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
    private static Map<Class<? extends ClientBoundPacket>, ClientSidePacketHandler<? extends ClientBoundPacket>> registeredPacketHandlers = Map.of();

    public static void initialize(@Nonnull Map<Class<? extends ClientBoundPacket>, ClientSidePacketHandler<? extends ClientBoundPacket>> map) {
        if (!INITIALIZED.compareAndSet(false, true)) {
            throw new IllegalStateException("ClientPacketHandlerRegistry already initialized");
        }

        registeredPacketHandlers = Map.copyOf(map);
    }

    public static Optional<ClientSidePacketHandler<? extends ClientBoundPacket>> get(@Nonnull Class<? extends ClientBoundPacket> packetClass) {
        return Optional.ofNullable(registeredPacketHandlers.get(packetClass));
    }

    public static boolean contains(@Nonnull Class<? extends ClientBoundPacket> packetClass) {
        return registeredPacketHandlers.containsKey(packetClass);
    }

}
