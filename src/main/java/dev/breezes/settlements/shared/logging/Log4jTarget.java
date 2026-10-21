package dev.breezes.settlements.shared.logging;

import dev.breezes.settlements.shared.logging.levels.LogLevel;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.spi.ExtendedLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Normal-log adapter for one logger in one category: forwards calls to the host's Log4j logger under the category's
 * marker, with the original throwable.
 */
final class Log4jTarget {

    // Log4j takes the caller's location from the frame after the last frame of the named class.
    // Below a handle method, that frame is the call site.
    private static final String CALLER_BOUNDARY = LoggerHandle.class.getName();

    private final ExtendedLogger logger;

    @Nullable
    private final Marker marker;

    Log4jTarget(@Nonnull ExtendedLogger logger, @Nonnull LogCategory category) {
        this.logger = logger;
        this.marker = category.getMarkerName().map(MarkerManager::getMarker).orElse(null);
    }

    /**
     * Forwards one call to Log4j with its arguments intact, preserving trailing-throwable handling and host filtering.
     */
    void forward(@Nonnull LogLevel level, @Nonnull String format, @Nonnull Object[] args) {
        this.logger.logIfEnabled(CALLER_BOUNDARY, toLog4j(level), this.marker, format, args);
    }

    private static Level toLog4j(LogLevel level) {
        return switch (level) {
            case TRACE -> Level.TRACE;
            case DEBUG -> Level.DEBUG;
            case INFO -> Level.INFO;
            case WARN -> Level.WARN;
            case ERROR -> Level.ERROR;
        };
    }

}
