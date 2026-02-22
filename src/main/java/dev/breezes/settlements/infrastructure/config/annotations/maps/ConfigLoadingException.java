package dev.breezes.settlements.infrastructure.config.annotations.maps;

import javax.annotation.Nonnull;

public class ConfigLoadingException extends RuntimeException {

    public ConfigLoadingException(@Nonnull String message) {
        super(message);
    }

    public ConfigLoadingException(@Nonnull String message, @Nonnull Throwable cause) {
        super(message, cause);
    }

}
