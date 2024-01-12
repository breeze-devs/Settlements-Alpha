package dev.breezes.settlements.util;

public class TimeUtil {

    private static final int TICKS_PER_SECOND = 20;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MINUTES_PER_HOUR = 60;

    public static int ticks(int ticks) {
        return ticks;
    }

    public static int seconds(int seconds) {
        return seconds * TICKS_PER_SECOND;
    }

    public static int minutes(int minutes) {
        return minutes * SECONDS_PER_MINUTE * TICKS_PER_SECOND;
    }

    public static int hours(int hours) {
        return hours * MINUTES_PER_HOUR * SECONDS_PER_MINUTE * TICKS_PER_SECOND;
    }

}
