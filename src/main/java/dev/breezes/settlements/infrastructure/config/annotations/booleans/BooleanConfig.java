package dev.breezes.settlements.infrastructure.config.annotations.booleans;

import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
public @interface BooleanConfig {

    ConfigurationType type();

    String identifier();

    String description();

    boolean defaultValue();

}
