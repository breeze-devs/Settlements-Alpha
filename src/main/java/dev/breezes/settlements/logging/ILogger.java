package dev.breezes.settlements.logging;

import dev.breezes.settlements.logging.levels.LogLevel;

import javax.annotation.Nonnull;

/**
 * Wrapper for all logging in Settlements
 */
public interface ILogger {

    void log(@Nonnull LogLevel level, @Nonnull String format, @Nonnull Object... args);

    /**
     * Logs a message at the default level
     */
    void log(@Nonnull String format, @Nonnull Object... args);

    void trace(@Nonnull String format, @Nonnull Object... args);

    void debug(@Nonnull String format, @Nonnull Object... args);

    void info(@Nonnull String format, @Nonnull Object... args);

    void warn(@Nonnull String format, @Nonnull Object... args);

    void error(@Nonnull String format, @Nonnull Object... args);

    void error(@Nonnull Throwable e, @Nonnull String format, @Nonnull Object... args);

}
