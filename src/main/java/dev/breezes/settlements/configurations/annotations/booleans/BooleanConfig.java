package dev.breezes.settlements.configurations.annotations.booleans;

import dev.breezes.settlements.configurations.annotations.ConfigurationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface BooleanConfig {

    ConfigurationType type();

    String identifier();

    String description();

    boolean defaultValue();

}
