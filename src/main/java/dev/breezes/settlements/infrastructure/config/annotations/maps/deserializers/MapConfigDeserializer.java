package dev.breezes.settlements.infrastructure.config.annotations.maps.deserializers;

import javax.annotation.Nonnull;

public interface MapConfigDeserializer<T> {

    T deserialize(@Nonnull String value);

    Class<T> getType();

}
