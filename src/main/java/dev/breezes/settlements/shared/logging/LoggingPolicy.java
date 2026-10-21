package dev.breezes.settlements.shared.logging;

import dev.breezes.settlements.shared.logging.levels.LogThreshold;

import java.util.Objects;

/**
 * Settings Settlements logging follows for a whole game run.
 *
 * @param enabled           master switch: while false, no level reaches any destination
 * @param standardThreshold the least severe level forwarded to normal logging
 */
public record LoggingPolicy(boolean enabled, LogThreshold standardThreshold) {

    /**
     * The policy with every setting at its default. Loggers follow it until a policy is installed.
     */
    public static final LoggingPolicy DEFAULT = new LoggingPolicy(true, LogThreshold.INFO);

    public LoggingPolicy {
        Objects.requireNonNull(standardThreshold, "standardThreshold");
    }

}
