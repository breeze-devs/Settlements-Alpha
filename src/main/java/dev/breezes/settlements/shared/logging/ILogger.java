package dev.breezes.settlements.shared.logging;

import dev.breezes.settlements.shared.logging.levels.LogLevel;

import javax.annotation.Nonnull;

/**
 * Logger bound to one category, through which Settlements code logs.
 * <p>
 * Each severity method takes a format whose {} placeholders take the arguments in order.
 * Pass an exception as the last argument without a corresponding placeholder to log it with its stack trace.
 */
public interface ILogger {

    void trace(@Nonnull String format, @Nonnull Object... args);

    void debug(@Nonnull String format, @Nonnull Object... args);

    void info(@Nonnull String format, @Nonnull Object... args);

    void warn(@Nonnull String format, @Nonnull Object... args);

    void error(@Nonnull String format, @Nonnull Object... args);

    /**
     * Returns whether Settlements admits a call at the given level, enabling preemptive filtering for expensive arguments.
     * The host's Log4j configuration can still drop an admitted call.
     */
    boolean isEnabled(@Nonnull LogLevel level);

    /**
     * Returns this logger in the given category: the same name and destinations, with its events in that category.
     * Every call for a category returns the same instance, so calling it on a logging path allocates nothing.
     */
    ILogger as(@Nonnull LogCategory category);

}
