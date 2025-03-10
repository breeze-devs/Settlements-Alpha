package dev.breezes.settlements.annotations.configurations.declarations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface StringConfig {

    String identifier();

    String description();

    String defaultValue();

}
