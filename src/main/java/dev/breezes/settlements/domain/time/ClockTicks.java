package dev.breezes.settlements.domain.time;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * Real-world elapsed time expressed in server ticks (20 ticks per real second).
 * <p>
 * Use this class for durations anchored to real elapsed time — behavior cooldowns,
 * animation lengths, or anything that should feel the same regardless of the in-game clock.
 * <p>
 * For durations that live on the Minecraft day cycle (plan slot times, schedule offsets),
 * use {@link GameTicks} instead, where 1,000 ticks equal one game hour.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class ClockTicks {

    public static final int TICKS_PER_SECOND = 20;
    public static final int SECONDS_PER_MINUTE = 60;
    public static final int MINUTES_PER_HOUR = 60;

    public static final ClockTicks ZERO = new ClockTicks(0);
    public static final ClockTicks ONE = new ClockTicks(1);

    private final long ticks;

    public static ClockTicks of(long ticks) {
        if (ticks == 0) {
            return ZERO;
        }
        if (ticks == 1) {
            return ONE;
        }
        return new ClockTicks(ticks);
    }

    public static ClockTicks seconds(double seconds) {
        return of((long) (seconds * TICKS_PER_SECOND));
    }

    public static ClockTicks minutes(double minutes) {
        return of((long) (minutes * SECONDS_PER_MINUTE * TICKS_PER_SECOND));
    }

    public static ClockTicks hours(double hours) {
        return of((long) (hours * MINUTES_PER_HOUR * SECONDS_PER_MINUTE * TICKS_PER_SECOND));
    }

    public int getTicksAsInt() {
        return Math.toIntExact(this.ticks);
    }

    public Tickable asTickable() {
        return Tickable.of(this);
    }

    public GameTicks asGameTicks() {
        return GameTicks.of(this.getTicksAsInt());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ClockTicks ticksValue)) {
            return false;
        }
        return this.ticks == ticksValue.ticks;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.ticks);
    }

    @Override
    public String toString() {
        return "ClockTicks{" +
                "ticks=" + this.ticks +
                '}';
    }

}
