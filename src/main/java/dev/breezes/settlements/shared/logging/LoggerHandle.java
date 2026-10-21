package dev.breezes.settlements.shared.logging;

import dev.breezes.settlements.shared.logging.levels.LogLevel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.logging.log4j.spi.ExtendedLogger;

import javax.annotation.Nonnull;

/**
 * Logger for one name in one category.
 * <p>
 * A handle is safe to keep in a static field from class initialization on.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
final class LoggerHandle implements ILogger {

    @Getter(AccessLevel.PACKAGE)
    private final String name;

    @Getter(AccessLevel.PACKAGE)
    private final LogCategory category;

    // This logger in every category, indexed by category ordinal
    // Every view in the family, this one included, shares the one array.
    private final LoggerHandle[] views;

    @Getter(AccessLevel.PACKAGE)
    private final Log4jTarget normalLog;

    private final LoggingBridge bridge;

    /**
     * Creates a handle forwarding to the given Log4j logger and routed through the given bridge, together with its
     * views in every other category.
     */
    static LoggerHandle create(@Nonnull ExtendedLogger log4jLogger,
                               @Nonnull LogCategory category,
                               @Nonnull LoggingBridge bridge) {
        LogCategory[] categories = LogCategory.values();
        LoggerHandle[] views = new LoggerHandle[categories.length];
        for (LogCategory viewCategory : categories) {
            Log4jTarget normalLog = new Log4jTarget(log4jLogger, viewCategory);
            views[viewCategory.ordinal()] = new LoggerHandle(log4jLogger.getName(), viewCategory, views, normalLog, bridge);
        }
        return views[category.ordinal()];
    }

    @Override
    public void trace(@Nonnull String format, @Nonnull Object... args) {
        this.emit(LogLevel.TRACE, format, args);
    }

    @Override
    public void debug(@Nonnull String format, @Nonnull Object... args) {
        this.emit(LogLevel.DEBUG, format, args);
    }

    @Override
    public void info(@Nonnull String format, @Nonnull Object... args) {
        this.emit(LogLevel.INFO, format, args);
    }

    @Override
    public void warn(@Nonnull String format, @Nonnull Object... args) {
        this.emit(LogLevel.WARN, format, args);
    }

    @Override
    public void error(@Nonnull String format, @Nonnull Object... args) {
        this.emit(LogLevel.ERROR, format, args);
    }

    @Override
    public boolean isEnabled(@Nonnull LogLevel level) {
        return this.bridge.route().admits(level);
    }

    @Override
    public ILogger as(@Nonnull LogCategory category) {
        return this.views[category.ordinal()];
    }

    private void emit(LogLevel level, String format, Object[] args) {
        // One read of the route, so admission and dispatch agree even while another route is being installed.
        LogRoute route = this.bridge.route();
        if (route.admits(level)) {
            route.dispatch(this, level, format, args);
        }
    }

}
