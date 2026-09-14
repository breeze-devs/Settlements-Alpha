package dev.breezes.settlements.shared.util;

import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

/**
 * Monotonic real time for client timers that must remain consistent across world changes.
 */
@ClientSide
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientMonotonicClock {

    private static final long MILLIS_PER_SECOND = 1_000L;

    /**
     * Returns milliseconds from an arbitrary origin, comparable only within the running JVM.
     */
    public static long nowMillis() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
    }

    public static long millisIn(@Nonnull ClockTicks duration) {
        return (long) duration.getTicksAsInt() * MILLIS_PER_SECOND / ClockTicks.TICKS_PER_SECOND;
    }

    public static long deadlineFrom(@Nonnull ClockTicks interval) {
        return nowMillis() + millisIn(interval);
    }

}
