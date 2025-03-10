package dev.breezes.settlements.annotations.configurations.declarations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface FloatConfig {

    String identifier();

    String description();

    float defaultValue();

    float min() default Float.MIN_VALUE;

    float max() default Float.MAX_VALUE;

}
