package dev.breezes.settlements.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
@Getter
public final class TimePeriod {

    public static final int TICKS_PER_SECOND = 20;
    public static final int SECONDS_PER_MINUTE = 60;
    public static final int MINUTES_PER_HOUR = 60;

    private final long ticks;

    public static TimePeriod ticks(long ticks) {
        return new TimePeriod(ticks);
    }

    public static TimePeriod seconds(long seconds) {
        return new TimePeriod(seconds * TICKS_PER_SECOND);
    }

    public static TimePeriod minutes(long minutes) {
        return new TimePeriod(minutes * SECONDS_PER_MINUTE * TICKS_PER_SECOND);
    }

    public static TimePeriod hours(long hours) {
        return new TimePeriod(hours * MINUTES_PER_HOUR * SECONDS_PER_MINUTE * TICKS_PER_SECOND);
    }

    public long getSeconds() {
        return ticks / TICKS_PER_SECOND;
    }

    public long getMinutes() {
        return ticks / (TICKS_PER_SECOND * SECONDS_PER_MINUTE);
    }

    public long getHours() {
        return ticks / (TICKS_PER_SECOND * SECONDS_PER_MINUTE * MINUTES_PER_HOUR);
    }

}
