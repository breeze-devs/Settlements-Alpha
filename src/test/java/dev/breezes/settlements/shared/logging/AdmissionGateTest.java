package dev.breezes.settlements.shared.logging;

import dev.breezes.settlements.shared.logging.levels.LogLevel;
import dev.breezes.settlements.shared.logging.levels.LogThreshold;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdmissionGateTest {

    private String loggerName;

    @BeforeEach
    void nameLoggerAfterTest(TestInfo testInfo) {
        // The display name tells a parameterized method's invocations apart.
        this.loggerName = AdmissionGateTest.class.getName() + "." + testInfo.getTestMethod().orElseThrow().getName()
                + testInfo.getDisplayName();
    }

    @ParameterizedTest
    @EnumSource(value = LogThreshold.class, names = "OFF", mode = EnumSource.Mode.EXCLUDE)
    void threshold_forwardsItsOwnLevelAndEveryMoreSevereLevel_only(LogThreshold threshold) {
        // Counterexample: reordered LogLevel constants, a neighboring threshold mapping, or a strict comparison
        // causes the gate to admit the wrong severities. Log4j's severity order is independent of LogLevel's ordinals.
        Level minimumLevel = Level.valueOf(threshold.name());
        List<Level> expected = Arrays.stream(LogLevel.values())
                .map(level -> Level.valueOf(level.name()))
                .filter(level -> level.isMoreSpecificThan(minimumLevel))
                .toList();
        ILogger log = this.loggerFollowing(new LoggingPolicy(true, threshold));

        try (CapturedLog4jEvents captured = CapturedLog4jEvents.forLogger(this.loggerName)) {
            for (LogLevel level : LogLevel.values()) {
                logAt(log, level);
            }

            assertEquals(expected, captured.levels());
        }
    }

    @Test
    void standardThresholdOff_forwardsNothing() {
        // Counterexample: OFF is read as the most severe level rather than as no level, so errors still reach Log4j.
        ILogger log = this.loggerFollowing(new LoggingPolicy(true, LogThreshold.OFF));

        try (CapturedLog4jEvents captured = CapturedLog4jEvents.forLogger(this.loggerName)) {
            logEveryCall(log);

            assertEquals(List.of(), captured.levels());
        }
    }

    @ParameterizedTest
    @EnumSource(LogThreshold.class)
    void masterSwitchOff_forwardsNothing_whateverTheThreshold(LogThreshold threshold) {
        // Counterexample: the master switch silences only levels below WARN, or only isEnabled, so warnings and errors
        // still reach Log4j with enabled = false.
        ILogger log = this.loggerFollowing(new LoggingPolicy(false, threshold));

        try (CapturedLog4jEvents captured = CapturedLog4jEvents.forLogger(this.loggerName)) {
            logEveryCall(log);

            assertEquals(List.of(), captured.levels());
        }
    }

    @Test
    void rejectedCall_neverRendersItsArguments() {
        // Counterexample: the handle renders the message, or captures its arguments, before the gate decides, so a
        // rejected call still pays for formatting.
        RenderCountingArgument argument = new RenderCountingArgument();
        ILogger log = this.loggerFollowing(new LoggingPolicy(true, LogThreshold.ERROR));

        try (CapturedLog4jEvents captured = CapturedLog4jEvents.forLogger(this.loggerName)) {
            log.warn("Settlement {} lost its bell", argument);
            log.warn("Settlement {} lost its bell", argument, new IllegalStateException("bell"));

            assertEquals(0, argument.renders);
            assertEquals(List.of(), captured.levels());
        }
    }

    static Stream<LoggingPolicy> everyPolicy() {
        return Stream.of(true, false)
                .flatMap(enabled -> Arrays.stream(LogThreshold.values())
                        .map(threshold -> new LoggingPolicy(enabled, threshold)));
    }

    @ParameterizedTest
    @MethodSource("everyPolicy")
    void isEnabled_answersWhetherACallAtThatLevelIsForwarded(LoggingPolicy policy) {
        // Counterexample: isEnabled consults a different threshold from the gate, so a caller guarding with it skips
        // lines that would be logged, or builds arguments for lines that are dropped.
        ILogger log = this.loggerFollowing(policy);
        List<Level> enabledLevels = Arrays.stream(LogLevel.values())
                .filter(log::isEnabled)
                .map(level -> Level.valueOf(level.name()))
                .toList();

        try (CapturedLog4jEvents captured = CapturedLog4jEvents.forLogger(this.loggerName)) {
            for (LogLevel level : LogLevel.values()) {
                logAt(log, level);
            }

            assertEquals(enabledLevels, captured.levels());
        }
    }

    private ILogger loggerFollowing(LoggingPolicy policy) {
        LoggingBridge bridge = new LoggingBridge(LogRoute.of(policy));
        return LoggerHandle.create(LogManager.getContext(false).getLogger(this.loggerName), LogCategory.GENERAL, bridge);
    }

    private static void logAt(ILogger log, LogLevel level) {
        switch (level) {
            case TRACE -> log.trace("Call at {}", level);
            case DEBUG -> log.debug("Call at {}", level);
            case INFO -> log.info("Call at {}", level);
            case WARN -> log.warn("Call at {}", level);
            case ERROR -> log.error("Call at {}", level);
        }
    }

    private static void logEveryCall(ILogger log) {
        IllegalStateException failure = new IllegalStateException("failure");
        log.trace("Call at {}", LogLevel.TRACE);
        log.trace("Call at {}", LogLevel.TRACE, failure);
        log.debug("Call at {}", LogLevel.DEBUG);
        log.debug("Call at {}", LogLevel.DEBUG, failure);
        log.info("Call at {}", LogLevel.INFO);
        log.info("Call at {}", LogLevel.INFO, failure);
        log.warn("Call at {}", LogLevel.WARN);
        log.warn("Call at {}", LogLevel.WARN, failure);
        log.error("Call at {}", LogLevel.ERROR);
        log.error("Call at {}", LogLevel.ERROR, failure);
    }

    private static final class RenderCountingArgument {

        private int renders;

        @Override
        public String toString() {
            this.renders++;
            return "argument";
        }

    }

}
