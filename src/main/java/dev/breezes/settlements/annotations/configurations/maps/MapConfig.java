package dev.breezes.settlements.annotations.configurations.maps;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface MapConfig {

    String identifier();

    String description();

    /**
     * See {@link dev.breezes.settlements.annotations.configurations.maps.deserializers.MapConfigDeserializer}
     */
    String deserializer() default "StringToString";

    MapEntry[] defaultValue();

}
