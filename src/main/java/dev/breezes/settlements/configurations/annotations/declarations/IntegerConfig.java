package dev.breezes.settlements.configurations.annotations.declarations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface IntegerConfig {

    String identifier();

    String description();

    int defaultValue();

    int min() default Integer.MIN_VALUE;

    int max() default Integer.MAX_VALUE;

}