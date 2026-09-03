package dev.breezes.settlements.infrastructure.rendering.debug.tuning;

import dev.breezes.settlements.domain.presentation.Socket;
import dev.breezes.settlements.domain.presentation.SocketId;
import dev.breezes.settlements.domain.presentation.SocketRegistry;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Wraps a {@link SocketRegistry} so named sockets resolve through a live override instead of their
 * authored placement. Sockets with no override resolve unchanged.
 */
@ClientSide
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class DebugTuningSocketRegistry implements SocketRegistry {

    private final SocketRegistry delegate;
    private final Map<SocketId, UnaryOperator<Socket>> overridesBySocket;

    public static SocketRegistry wrapping(@Nonnull SocketRegistry delegate,
                                          @Nonnull Map<SocketId, UnaryOperator<Socket>> overridesBySocket) {
        return new DebugTuningSocketRegistry(delegate, Map.copyOf(overridesBySocket));
    }

    @Override
    public Socket get(@Nonnull SocketId id) {
        Socket socket = this.delegate.get(id);
        UnaryOperator<Socket> override = this.overridesBySocket.get(id);
        return override == null ? socket : override.apply(socket);
    }

}
