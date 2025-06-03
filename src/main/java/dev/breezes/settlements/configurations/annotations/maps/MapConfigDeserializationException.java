package dev.breezes.settlements.configurations.annotations.maps;

import javax.annotation.Nonnull;

public class MapConfigDeserializationException extends RuntimeException {

    public MapConfigDeserializationException(@Nonnull String expectedType,
                                             @Nonnull String rawValue) {
        super("Failed to deserialize '%s' to %s".formatted(rawValue, expectedType));
    }

}
