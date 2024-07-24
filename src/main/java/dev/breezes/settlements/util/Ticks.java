package dev.breezes.settlements.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
@Getter
public class Ticks {

    public static final int TICKS_PER_SECOND = 20;
    public static final int SECONDS_PER_MINUTE = 60;
    public static final int MINUTES_PER_HOUR = 60;

    private final long ticks;

    public static Ticks of(long ticks) {
        return new Ticks(ticks);
    }

    public static Ticks fromSeconds(long seconds) {
        return new Ticks(seconds * TICKS_PER_SECOND);
    }

    public static Ticks fromMinutes(long minutes) {
        return new Ticks(minutes * SECONDS_PER_MINUTE * TICKS_PER_SECOND);
    }

    public static Ticks fromHours(long hours) {
        return new Ticks(hours * MINUTES_PER_HOUR * SECONDS_PER_MINUTE * TICKS_PER_SECOND);
    }

}
