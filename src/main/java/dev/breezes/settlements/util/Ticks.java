package dev.breezes.settlements.util;

import dev.breezes.settlements.models.misc.Tickable;
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

    public static Ticks seconds(double seconds) {
        return new Ticks((long) (seconds * TICKS_PER_SECOND));
    }

    public static Ticks minutes(double minutes) {
        return new Ticks((long) (minutes * SECONDS_PER_MINUTE * TICKS_PER_SECOND));
    }

    public static Ticks hours(double hours) {
        return new Ticks((long) (hours * MINUTES_PER_HOUR * SECONDS_PER_MINUTE * TICKS_PER_SECOND));
    }

    public static Ticks one() {
        return new Ticks(1);
    }

    public int getTicksAsInt() {
        return (int) ticks;
    }

    public Tickable asTickable() {
        return Tickable.of(this);
    }

}
