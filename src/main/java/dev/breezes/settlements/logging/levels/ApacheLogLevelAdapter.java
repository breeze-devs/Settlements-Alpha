package dev.breezes.settlements.logging.levels;

import org.apache.logging.log4j.Level;

public class ApacheLogLevelAdapter {

    public static Level adapt(LogLevel level) {
        return switch (level) {
            case TRACE -> Level.TRACE;
            case DEBUG -> Level.DEBUG;
            case WARN -> Level.WARN;
            case ERROR -> Level.ERROR;
            default -> Level.INFO;
        };
    }

}
