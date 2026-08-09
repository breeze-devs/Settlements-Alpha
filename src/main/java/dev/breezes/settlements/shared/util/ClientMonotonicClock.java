package dev.breezes.settlements.shared.util;

import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

/**
 * Real-elapsed clock for client cadences that belong to the process rather than to a world.
 * <p>
 * A client-side throttle or timeout must not be gated on Level#getGameTime(). That clock is per-save, so it
 * runs backwards when the player leaves one world for another with a lower game time, and a deadline stored
 * from the first world then sits permanently in the second world's future — the throttled work simply never
 * runs again for the rest of the process, silently and without an exception.
 * <p>
 * Durations stay in {@link ClockTicks}: the cadence a caller wants to express is still a domain quantity, and
 * only the instant it is measured against belongs to this clock.
 */
@ClientSide
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientMonotonicClock {

    private static final long MILLIS_PER_SECOND = 1_000L;

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
