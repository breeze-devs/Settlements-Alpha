package dev.breezes.settlements.domain.presentation;

import javax.annotation.Nonnull;

/**
 * Resolves named attachment points. Add new attach locations here instead of teaching render layers about slots.
 */
public interface SocketRegistry {

    Socket get(@Nonnull SocketId id);

}
