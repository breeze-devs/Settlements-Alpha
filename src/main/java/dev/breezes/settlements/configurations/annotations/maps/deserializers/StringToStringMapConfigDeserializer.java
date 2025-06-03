package dev.breezes.settlements.configurations.annotations.maps.deserializers;

import javax.annotation.Nonnull;

public class StringToStringMapConfigDeserializer implements MapConfigDeserializer<String> {

    @Override
    public String deserialize(@Nonnull String value) {
        return value;
    }

    @Override
    public Class<String> getType() {
        return String.class;
    }

}
