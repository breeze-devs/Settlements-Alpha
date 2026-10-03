package dev.breezes.settlements.domain.time;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TickableTest {

    private static final ClockTicks INTERVAL = ClockTicks.of(5);
    private static final int SAMPLE_SIZE = 200;
    private static final long SEED = 42L;

    @Test
    void staggered_firstFiringsCoverEveryTickOfTheFirstInterval_andNoOther() {
        // Arrange -- the sample is large enough that a seeded draw reaches every tick of the interval.
        int interval = INTERVAL.getTicksAsInt();
        Random random = new Random(SEED);
        Set<Integer> firstFirings = new HashSet<>();

        // Act
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            firstFirings.add(ticksUntilFirstCompletion(Tickable.staggered(INTERVAL, random), interval));
        }

        // Assert -- counterexamples: an unstaggered cadence fires every timer on the same tick; a draw
        // from [0, interval) never first-fires on the interval's last tick; a draw wider than the
        // interval delays a fresh timer's first firing past it.
        Set<Integer> everyTickOfTheInterval = IntStream.rangeClosed(1, interval).boxed().collect(Collectors.toSet());
        assertEquals(everyTickOfTheInterval, firstFirings);
    }

    @Test
    void staggered_reArmsAtExactlyTheInterval_afterTheStaggeredFirstFiring() {
        // Arrange
        int interval = INTERVAL.getTicksAsInt();
        Tickable cooldown = Tickable.staggered(INTERVAL, new Random(SEED));
        ticksUntilFirstCompletion(cooldown, interval);

        // Act & Assert -- counterexample: a timer that re-arms before or after exactly the interval
        // following its staggered first firing.
        for (int tick = 1; tick < interval; tick++) {
            assertFalse(cooldown.tickCheckAndReset(1), "must not re-arm before the interval elapses");
        }
        assertTrue(cooldown.tickCheckAndReset(1), "must re-arm once the interval elapses");
    }

    @Test
    void staggered_anIntervalBelowOneTick_firesEveryTick() {
        // Arrange -- counterexample: drawing from an empty range throws instead of clamping the
        // interval to one tick.
        Tickable cooldown = Tickable.staggered(ClockTicks.ZERO, new Random(SEED));

        // Act & Assert
        assertTrue(cooldown.tickCheckAndReset(1), "must fire on the first tick");
        assertTrue(cooldown.tickCheckAndReset(1), "must fire again on the next tick");
    }

    @Test
    void staggeredPhase_wrapsANegativeIdentityHashIntoThePositiveRange() {
        // Arrange -- counterexample: a plain % instead of a floor-mod leaves the phase negative for
        // roughly half of all identity hashes.
        ClockTicks interval = ClockTicks.of(7);

        // Act
        long phase = Tickable.staggeredPhase(interval, -3);

        // Assert
        assertTrue(phase >= 0 && phase < interval.getTicks(), "phase must land in [0, interval)");
    }

    @Test
    void staggeredPhase_clampsAnIntervalBelowOneTick_toOneTick() {
        // Arrange & Act -- counterexample: a floor-mod by zero throws ArithmeticException instead of
        // clamping to a safe minimum interval.
        long phase = Tickable.staggeredPhase(ClockTicks.ZERO, 123);

        // Assert -- anything mod 1 is 0, independent of how the clamp is implemented.
        assertEquals(0L, phase);
    }

    private static int ticksUntilFirstCompletion(ITickable cooldown, int interval) {
        // Bounded well past one interval so a runaway stagger fails the assertion, not the build.
        for (int tick = 1; tick <= interval * 4; tick++) {
            if (cooldown.tickCheckAndReset(1)) {
                return tick;
            }
        }
        return Integer.MAX_VALUE;
    }

}
