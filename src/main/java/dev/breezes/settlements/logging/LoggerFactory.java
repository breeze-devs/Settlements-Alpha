package dev.breezes.settlements.logging;

import javax.annotation.Nonnull;

public class LoggerFactory {

    public static ILogger getLogger(@Nonnull String className) {
        try {
            return new ApacheLogger(Class.forName(className));
        } catch (ClassNotFoundException e) {
            return new ApacheLogger(className);
        }
    }

}
