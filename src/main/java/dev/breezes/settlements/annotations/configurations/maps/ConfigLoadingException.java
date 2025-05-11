package dev.breezes.settlements.annotations.configurations.maps;

import javax.annotation.Nonnull;

public class ConfigLoadingException extends RuntimeException {

    public ConfigLoadingException(@Nonnull String message, @Nonnull Throwable cause) {
        super(message, cause);
    }

}
