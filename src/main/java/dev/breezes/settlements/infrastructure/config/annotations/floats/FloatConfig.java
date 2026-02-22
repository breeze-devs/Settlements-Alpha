package dev.breezes.settlements.infrastructure.config.annotations.floats;

import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
public @interface FloatConfig {

    ConfigurationType type();

    String identifier();

    String description();

    float defaultValue();

    float min() default Float.MIN_VALUE;

    float max() default Float.MAX_VALUE;

}
