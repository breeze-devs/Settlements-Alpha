package dev.breezes.settlements.infrastructure.logging;

import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.booleans.BooleanConfig;
import dev.breezes.settlements.infrastructure.config.annotations.enums.EnumConfig;
import dev.breezes.settlements.shared.logging.LoggingPolicy;
import dev.breezes.settlements.shared.logging.levels.LogThreshold;

/**
 * The logging section of the debugging configuration, read once during mod construction.
 */
@BehaviorConfig(name = "logging", type = ConfigurationType.DEBUGGING)
public record LoggingConfig(

        @BooleanConfig(
                type = ConfigurationType.DEBUGGING,
                identifier = "enabled",
                description = "Master switch for Settlements routine logging. " +
                        "When false, Settlements logs nothing, warnings and errors included. " +
                        "Logging by Minecraft, NeoForge, and other mods is unaffected. " +
                        "Changes apply after a game restart",
                defaultValue = true)
        boolean enabled,

        @EnumConfig(
                identifier = "standard_log_level",
                description = "Least severe level of Settlements messages sent to the game's logs, or OFF to disable. " +
                        "Changes apply after a game restart",
                defaultValue = "INFO")
        LogThreshold standardLogLevel

) {

    /**
     * Returns the logging policy these settings describe.
     */
    public LoggingPolicy toPolicy() {
        return new LoggingPolicy(this.enabled, this.standardLogLevel);
    }

}
