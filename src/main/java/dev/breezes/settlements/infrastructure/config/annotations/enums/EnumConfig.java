package dev.breezes.settlements.infrastructure.config.annotations.enums;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configures a record component of an enum type. The file accepts only the enum's constant names, in any letter case,
 * and NeoForge corrects any other value to the default.
 * <p>
 * Record components only: the record's own configuration declares which file it belongs to.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.RECORD_COMPONENT)
public @interface EnumConfig {

    String identifier();

    String description();

    /**
     * Name of the default constant.
     */
    String defaultValue();

}
