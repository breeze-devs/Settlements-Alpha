package dev.breezes.settlements.shared.util;

import dev.breezes.settlements.domain.time.ClockTicks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientMonotonicClockTest {

    @Test
    void millisIn_convertsWholeSecondsToWallClockMilliseconds() {
        ClockTicks twoSeconds = ClockTicks.seconds(2);

        long millis = ClientMonotonicClock.millisIn(twoSeconds);

        assertEquals(2_000L, millis);
    }

    @Test
    void millisIn_convertsSubSecondIntervalsWithoutCollapsingToZero() {
        ClockTicks halfSecond = ClockTicks.seconds(0.5);

        long millis = ClientMonotonicClock.millisIn(halfSecond);

        assertEquals(500L, millis);
    }

    @Test
    void millisIn_doesNotOverflowForIntervalsBeyondIntMillisecondRange() {
        // Past roughly thirty hours the tick count multiplied by a thousand exceeds Integer.MAX_VALUE, so an
        // int-width multiply wraps negative — and a negative deadline is always already elapsed, which turns a
        // throttle into work that runs every single frame.
        ClockTicks beyondIntMillisRange = ClockTicks.hours(48);

        long millis = ClientMonotonicClock.millisIn(beyondIntMillisRange);

        assertEquals(172_800_000L, millis);
    }

}
