package dev.breezes.settlements.domain.exceptions;

import javax.annotation.Nonnull;

public class SpawnFailedException extends RuntimeException {

    public SpawnFailedException(String message) {
        super(message);
    }

    public SpawnFailedException(@Nonnull String message, @Nonnull Throwable cause) {
        super(message, cause);
    }

}
