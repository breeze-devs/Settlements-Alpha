package dev.breezes.settlements.configurations.annotations.strings;

import dev.breezes.settlements.configurations.annotations.ConfigurationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface StringConfig {

    ConfigurationType type();

    String identifier();

    String description();

    String defaultValue();

}
