package dev.breezes.settlements.configurations.annotations.doubles;

import dev.breezes.settlements.configurations.annotations.ConfigurationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
public @interface DoubleConfig {

    ConfigurationType type();

    String identifier();

    String description();

    double defaultValue();

    double min() default Double.MIN_VALUE;

    double max() default Double.MAX_VALUE;

}
