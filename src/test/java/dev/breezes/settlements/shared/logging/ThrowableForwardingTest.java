package dev.breezes.settlements.shared.logging;

import dev.breezes.settlements.shared.logging.levels.LogThreshold;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ThrowableForwardingTest {

    private String loggerName;

    private interface SeverityMethod {

        void log(ILogger logger, String format, Object... args);

    }

    static Stream<Arguments> levels() {
        return Stream.of(
                Arguments.of(Level.TRACE, (SeverityMethod) ILogger::trace),
                Arguments.of(Level.DEBUG, (SeverityMethod) ILogger::debug),
                Arguments.of(Level.INFO, (SeverityMethod) ILogger::info),
                Arguments.of(Level.WARN, (SeverityMethod) ILogger::warn),
                Arguments.of(Level.ERROR, (SeverityMethod) ILogger::error));
    }

    @BeforeEach
    void nameLoggerAfterTest(TestInfo testInfo) {
        this.loggerName = ThrowableForwardingTest.class.getName() + "." + testInfo.getTestMethod().orElseThrow().getName();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("levels")
    void trailingThrowable_isForwarded_withNoPlaceholders(Level expectedLevel, SeverityMethod severity) {
        // Counterexample: the adapter drops the unconsumed trailing throwable, so a bare message loses its stack trace.
        String loggerName = this.loggerName + "." + expectedLevel;
        ILogger log = loggerAdmittingEveryLevel(loggerName);
        IllegalStateException failure = new IllegalStateException("teardown");

        try (CapturedLog4jEvents captured = CapturedLog4jEvents.forLogger(loggerName)) {
            severity.log(log, "Teardown failed", failure);

            LogEvent event = captured.single();
            assertEquals(expectedLevel, event.getLevel());
            assertEquals("Teardown failed", event.getMessage().getFormattedMessage());
            assertSame(failure, event.getThrown());
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("levels")
    void trailingThrowable_isForwarded_afterOneSubstitutedArgument(Level expectedLevel, SeverityMethod severity) {
        // Counterexample: the argument array travels as one parameter, so the placeholder renders as an array.
        String loggerName = this.loggerName + "." + expectedLevel;
        ILogger log = loggerAdmittingEveryLevel(loggerName);
        IllegalStateException failure = new IllegalStateException("abort");

        try (CapturedLog4jEvents captured = CapturedLog4jEvents.forLogger(loggerName)) {
            severity.log(log, "Behavior '{}' abort cause", "harvest", failure);

            LogEvent event = captured.single();
            assertEquals(expectedLevel, event.getLevel());
            assertEquals("Behavior 'harvest' abort cause", event.getMessage().getFormattedMessage());
            assertSame(failure, event.getThrown());
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("levels")
    void trailingThrowable_isForwarded_afterSeveralSubstitutedArguments(Level expectedLevel, SeverityMethod severity) {
        // Counterexample: wrapping the ordinary arguments in another array leaves placeholders to consume the throwable.
        String loggerName = this.loggerName + "." + expectedLevel;
        ILogger log = loggerAdmittingEveryLevel(loggerName);
        IllegalStateException failure = new IllegalStateException("store");

        try (CapturedLog4jEvents captured = CapturedLog4jEvents.forLogger(loggerName)) {
            severity.log(log, "Listener {} failed on settlement {} in {}", "tracker", 7, "overworld", failure);

            LogEvent event = captured.single();
            assertEquals(expectedLevel, event.getLevel());
            assertEquals("Listener tracker failed on settlement 7 in overworld", event.getMessage().getFormattedMessage());
            assertSame(failure, event.getThrown());
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("levels")
    void trailingThrowable_isRenderedAsAnArgument_whenAPlaceholderConsumesIt(Level expectedLevel, SeverityMethod severity) {
        // Counterexample: eagerly extracting the trailing throwable bypasses the format's decision to consume it.
        String loggerName = this.loggerName + "." + expectedLevel;
        ILogger log = loggerAdmittingEveryLevel(loggerName);
        IllegalStateException failure = new IllegalStateException("conversion");

        try (CapturedLog4jEvents captured = CapturedLog4jEvents.forLogger(loggerName)) {
            severity.log(log, "Conversion failed for {}: {}", "villager", failure);

            LogEvent event = captured.single();
            assertEquals(expectedLevel, event.getLevel());
            assertEquals("Conversion failed for villager: " + failure, event.getMessage().getFormattedMessage());
            assertNull(event.getThrown());
        }
    }

    private static ILogger loggerAdmittingEveryLevel(String loggerName) {
        LoggingBridge bridge = new LoggingBridge(LogRoute.of(new LoggingPolicy(true, LogThreshold.TRACE)));
        return LoggerHandle.create(LogManager.getContext(false).getLogger(loggerName), LogCategory.GENERAL, bridge);
    }

}
