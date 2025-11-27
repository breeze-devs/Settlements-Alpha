package dev.breezes.settlements.configurations.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a Java Record as a behavior configuration object.
 * The annotated record's components will be processed by the
 * RecordConfigProcessor
 * to generate NeoForge configuration specifications and enable runtime binding.
 * <p>
 * Each component in the record should be annotated with a config type
 * annotation
 * (e.g., @IntegerConfig, @BooleanConfig, etc.).
 * <p>
 * Example:
 *
 * <pre>
 * {@code
 * @BehaviorConfig(name = "shear_sheep")
 * public record ShearSheepConfig(
 *     @IntegerConfig(...) int cooldownMin,
 *     @IntegerConfig(...) int cooldownMax
 * ) {}
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BehaviorConfig {

    /**
     * The unique identifier for this config.
     * This will be used as the config file name (e.g., "shear_sheep" -> "shear_sheep.toml")
     * and as the section name in hierarchical config structures.
     */
    String name();

    /**
     * The configuration type, which determines the config file location.
     * Defaults to BEHAVIOR.
     */
    ConfigurationType type() default ConfigurationType.BEHAVIOR;

}
