package dev.breezes.settlements.shared.logging;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.ExtendedLogger;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Creates the loggers Settlements code logs through and installs the policy they follow.
 * <p>
 * Creation is safe in a static initializer before the policy is installed.
 * Every logger created here follows the policy installed later, whenever it was created.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LoggerFactory {

    /**
     * Installs the policy every logger created here follows for the rest of the game run.
     * Until it is installed, they follow LoggingPolicy.DEFAULT.
     *
     * @throws IllegalStateException if a policy is already installed; the installed policy stays in force
     */
    public static void installPolicy(@Nonnull LoggingPolicy policy) {
        LoggingBridge.SHARED.installPolicy(policy);
    }

    /**
     * Returns a logger named after the class, in the general category.
     */
    public static ILogger getLogger(@Nonnull Class<?> type) {
        return LoggerHandle.create(log4jLogger(type), LogCategory.GENERAL, LoggingBridge.SHARED);
    }

    /**
     * Returns a logger named after the class, in the category with the given topic.
     *
     * @throws IllegalArgumentException if no category has the topic
     */
    public static ILogger getLogger(@Nonnull Class<?> type, @Nonnull String topic) {
        LogCategory category = LogCategory.forTopic(topic)
                .orElseThrow(() -> new IllegalArgumentException("Unknown log topic '%s' on %s; known topics are %s"
                        .formatted(topic, type.getName(), knownTopics())));
        return LoggerHandle.create(log4jLogger(type), category, LoggingBridge.SHARED);
    }

    /**
     * Returns a logger with the given name, in the general category.
     */
    public static ILogger getLogger(@Nonnull String name) {
        return LoggerHandle.create(LogManager.getContext(false).getLogger(name), LogCategory.GENERAL, LoggingBridge.SHARED);
    }

    private static ExtendedLogger log4jLogger(Class<?> type) {
        // LogManager.getLogger(Class)'s own lookup, which it returns only as a plain Logger: the class's loader selects
        // the Log4j context, and the logger takes the class's canonical name.
        return LogManager.getContext(type.getClassLoader(), false).getLogger(type);
    }

    private static String knownTopics() {
        return Arrays.stream(LogCategory.values())
                .map(LogCategory::getTopic)
                .collect(Collectors.joining(", "));
    }

}
