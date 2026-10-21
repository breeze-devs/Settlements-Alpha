package dev.breezes.settlements.shared.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Records every Log4j event one logger name produces in the active Log4j context, from creation
 * until close.
 * <p>
 * The logger name gets its own logger configuration at level ALL without additivity, so the record
 * is independent of the context's root level and appenders, and nothing it captures reaches them.
 * At most one capture may be open for a logger name at a time.
 */
final class CapturedLog4jEvents implements AutoCloseable {

    private final LoggerContext context;
    private final String loggerName;
    private final RecordingAppender appender;

    private CapturedLog4jEvents(LoggerContext context, String loggerName, RecordingAppender appender) {
        this.context = context;
        this.loggerName = loggerName;
        this.appender = appender;
    }

    /**
     * Starts recording the events of the named logger.
     */
    static CapturedLog4jEvents forLogger(String loggerName) {
        LoggerContext context = LoggerContext.getContext(false);
        Configuration configuration = context.getConfiguration();

        RecordingAppender appender = new RecordingAppender("captured:" + loggerName);
        appender.start();
        LoggerConfig loggerConfig = new LoggerConfig(loggerName, Level.ALL, false);
        loggerConfig.addAppender(appender, Level.ALL, null);
        configuration.addLogger(loggerName, loggerConfig);
        context.updateLoggers();

        return new CapturedLog4jEvents(context, loggerName, appender);
    }

    /**
     * Returns the level of every event recorded so far, in the order Log4j delivered them.
     */
    List<Level> levels() {
        return this.appender.events.stream()
                .map(LogEvent::getLevel)
                .toList();
    }

    /**
     * Returns the one event recorded so far, failing the test unless exactly one was.
     */
    LogEvent single() {
        assertEquals(1, this.appender.events.size(), "recorded events");
        return this.appender.events.getFirst();
    }

    /**
     * Returns the name of the event's marker, or empty when it carries none.
     */
    static Optional<String> markerName(LogEvent event) {
        return Optional.ofNullable(event.getMarker()).map(Marker::getName);
    }

    @Override
    public void close() {
        this.context.getConfiguration().removeLogger(this.loggerName);
        this.context.updateLoggers();
        this.appender.stop();
    }

    private static final class RecordingAppender extends AbstractAppender {

        private final List<LogEvent> events = new ArrayList<>();

        private RecordingAppender(String name) {
            super(name, null, null, true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            // Log4j may reuse a mutable event and its message once append returns.
            this.events.add(event.toImmutable());
        }

    }

}
