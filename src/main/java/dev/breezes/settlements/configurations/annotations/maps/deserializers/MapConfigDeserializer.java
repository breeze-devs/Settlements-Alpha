package dev.breezes.settlements.configurations.annotations.maps.deserializers;

import javax.annotation.Nonnull;

public interface MapConfigDeserializer<T> {

    T deserialize(@Nonnull String value);

    Class<T> getType();

}
