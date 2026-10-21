package dev.breezes.settlements.shared.logging;

import dev.breezes.settlements.shared.logging.levels.LogLevel;
import dev.breezes.settlements.shared.logging.levels.LogThreshold;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoggingBridgeTest {

    private String loggerName;

    @BeforeEach
    void nameLoggerAfterTest(TestInfo testInfo) {
        this.loggerName = LoggingBridgeTest.class.getName() + "." + testInfo.getTestMethod().orElseThrow().getName();
    }

    @Test
    void installedPolicy_governsHandlesCreatedBeforeTheInstall() {
        // Counterexample: a handle keeps the route it was created under, so static loggers initialized before the mod
        // constructor never follow the configured policy.
        LoggingBridge bridge = new LoggingBridge(LogRoute.of(new LoggingPolicy(true, LogThreshold.OFF)));
        ILogger log = this.loggerOn(bridge);

        bridge.installPolicy(new LoggingPolicy(true, LogThreshold.TRACE));

        try (CapturedLog4jEvents captured = CapturedLog4jEvents.forLogger(this.loggerName)) {
            log.trace("Villager schedule unchanged");

            assertEquals(List.of(Level.TRACE), captured.levels());
        }
    }

    @Test
    void secondInstall_isRejected_andTheFirstPolicyStaysInForce() {
        // Counterexample: a later install silently replaces the policy, so resolving the configuration again during
        // the game run changes logging although a change requires a restart.
        LoggingBridge bridge = new LoggingBridge(LogRoute.of(new LoggingPolicy(true, LogThreshold.OFF)));
        ILogger log = this.loggerOn(bridge);
        bridge.installPolicy(new LoggingPolicy(true, LogThreshold.ERROR));

        assertThrows(IllegalStateException.class, () -> bridge.installPolicy(new LoggingPolicy(true, LogThreshold.TRACE)));

        try (CapturedLog4jEvents captured = CapturedLog4jEvents.forLogger(this.loggerName)) {
            log.warn("Settlement lost its bell");
            log.error("Settlement lost its bell");

            assertEquals(List.of(Level.ERROR), captured.levels());
        }
    }

    @Test
    void sharedBridge_beforeAnyInstall_admitsWhatTheDefaultPolicyAdmits() {
        // Counterexample: the shared bridge starts by admitting every level, so DEBUG and TRACE from static initializers
        // reach Log4j before the configured policy is installed.
        LoggingBridge defaultInstalled = new LoggingBridge(LogRoute.of(new LoggingPolicy(true, LogThreshold.OFF)));
        defaultInstalled.installPolicy(LoggingPolicy.DEFAULT);

        for (LogLevel level : LogLevel.values()) {
            assertEquals(defaultInstalled.route().admits(level), LoggingBridge.SHARED.route().admits(level), level.name());
        }
    }

    private ILogger loggerOn(LoggingBridge bridge) {
        return LoggerHandle.create(LogManager.getContext(false).getLogger(this.loggerName), LogCategory.GENERAL, bridge);
    }

}
