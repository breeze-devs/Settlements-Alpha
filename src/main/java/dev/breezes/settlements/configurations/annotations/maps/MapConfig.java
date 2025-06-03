package dev.breezes.settlements.configurations.annotations.maps;

import dev.breezes.settlements.configurations.annotations.ConfigurationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface MapConfig {

    ConfigurationType type();

    String identifier();

    String description();

    /**
     * See {@link dev.breezes.settlements.configurations.annotations.maps.deserializers.MapConfigDeserializer}
     */
    String deserializer() default "StringToString";

    MapEntry[] defaultValue();

}
