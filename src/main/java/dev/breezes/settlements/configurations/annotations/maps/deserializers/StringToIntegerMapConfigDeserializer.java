package dev.breezes.settlements.configurations.annotations.maps.deserializers;

import dev.breezes.settlements.configurations.annotations.maps.MapConfigDeserializationException;

import javax.annotation.Nonnull;

public class StringToIntegerMapConfigDeserializer implements MapConfigDeserializer<Integer> {

    @Override
    public Integer deserialize(@Nonnull String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new MapConfigDeserializationException(value, "integer");
        }
    }

    @Override
    public Class<Integer> getType() {
        return Integer.class;
    }


}
