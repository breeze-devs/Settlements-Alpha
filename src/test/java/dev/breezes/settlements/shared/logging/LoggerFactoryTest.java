package dev.breezes.settlements.shared.logging;

import lombok.CustomLog;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static dev.breezes.settlements.shared.logging.CapturedLog4jEvents.markerName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggerFactoryTest {

    @CustomLog
    private static final class GeneralProducer {
    }

    @CustomLog(topic = "behavior")
    private static final class BehaviorProducer {
    }

    @CustomLog(topic = "behavior")
    private static final class ViewProducer {
    }

    @Test
    void classWithoutTopic_logsWithoutMarker() {
        // Counterexample: the general category sends a marker, so every general line in normal logs gains one.
        try (CapturedLog4jEvents captured = CapturedLog4jEvents.forLogger(GeneralProducer.class.getCanonicalName())) {
            GeneralProducer.log.info("Settlement founded");

            assertEquals(Optional.empty(), markerName(captured.single()));
        }
    }

    @Test
    void classWithTopic_marksEventsWithItsCategory() {
        // Counterexample: the Lombok declaration or the factory drops the topic, so the class logs in the general category.
        try (CapturedLog4jEvents captured = CapturedLog4jEvents.forLogger(BehaviorProducer.class.getCanonicalName())) {
            BehaviorProducer.log.info("Behavior started");

            assertEquals(LogCategory.BEHAVIOR.getMarkerName(), markerName(captured.single()));
        }
    }

    @Test
    void unknownTopic_failsNamingTheTopicAndTheClass() {
        // Counterexample: a misspelled topic falls back to the general category, or fails without saying where.
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> LoggerFactory.getLogger(LoggerFactoryTest.class, "no_such_topic"));

        assertTrue(failure.getMessage().contains("no_such_topic"), failure.getMessage());
        assertTrue(failure.getMessage().contains(LoggerFactoryTest.class.getName()), failure.getMessage());
    }

    @Test
    void as_returnsTheSameViewOnEveryCall() {
        // Counterexample: as() builds a new view per call, allocating on every logging call that uses it.
        ILogger log = LoggerFactory.getLogger(LoggerFactoryTest.class, "behavior");

        assertSame(log.as(LogCategory.SENSOR), log.as(LogCategory.SENSOR));
    }

    @Test
    void asView_logsUnderTheSameNameInTheRequestedCategory() {
        // Counterexample: the view reuses its origin's normal-log target, so its events keep the origin's marker.
        String loggerName = ViewProducer.class.getCanonicalName();
        try (CapturedLog4jEvents captured = CapturedLog4jEvents.forLogger(loggerName)) {
            ViewProducer.log.as(LogCategory.SENSOR).warn("Scan found no workstation");

            LogEvent event = captured.single();
            assertEquals(loggerName, event.getLoggerName());
            assertEquals(LogCategory.SENSOR.getMarkerName(), markerName(event));
        }
    }

}
