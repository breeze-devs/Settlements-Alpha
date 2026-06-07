package dev.breezes.settlements.domain.ai.planning;

import dev.breezes.settlements.domain.time.GameTicks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChronotypeTest {

    private static final int WAKE_SLEEP_RANGE = GameTicks.minutes(40).getTicksAsInt();
    private static final int MEAL_RANGE = GameTicks.minutes(12).getTicksAsInt();

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, 42L, -99L, Long.MAX_VALUE, Long.MIN_VALUE})
    void of_sameSeedProducesIdenticalOffsets(long seed) {
        // Arrange + Act
        Chronotype first = Chronotype.of(seed);
        Chronotype second = Chronotype.of(seed);

        // Assert — determinism is load-bearing; PlanRunner recomputes wake on every scheduling call
        assertEquals(first.wakeSleepOffsetTicks(), second.wakeSleepOffsetTicks());
        assertEquals(first.mealOffsetTicks(), second.mealOffsetTicks());
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, 42L, 99L, 1000L, Long.MAX_VALUE})
    void of_wakeSleepOffsetWithinBounds(long seed) {
        // Arrange + Act
        int offset = Chronotype.of(seed).wakeSleepOffsetTicks();

        // Assert
        assertTrue(offset >= -WAKE_SLEEP_RANGE && offset <= WAKE_SLEEP_RANGE,
                "Wake/sleep offset " + offset + " out of bounds ±" + WAKE_SLEEP_RANGE + " for seed=" + seed);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, 42L, 99L, 1000L, Long.MAX_VALUE})
    void of_mealOffsetWithinBounds(long seed) {
        // Arrange + Act
        int offset = Chronotype.of(seed).mealOffsetTicks();

        // Assert
        assertTrue(offset >= -MEAL_RANGE && offset <= MEAL_RANGE,
                "Meal offset " + offset + " out of bounds ±" + MEAL_RANGE + " for seed=" + seed);
    }

    @Test
    void of_differentSeedsGenerallyProduceDifferentOffsets() {
        // Arrange — use two clearly distinct seeds
        long seedA = 12345L;
        long seedB = 98765L;

        // Act
        Chronotype a = Chronotype.of(seedA);
        Chronotype b = Chronotype.of(seedB);

        // Assert — with high probability distinct seeds produce at least one differing offset
        boolean differ = a.wakeSleepOffsetTicks() != b.wakeSleepOffsetTicks()
                || a.mealOffsetTicks() != b.mealOffsetTicks();
        assertTrue(differ, "Two distinct seeds produced identical chronotypes — check seeding.");
    }

    @Test
    void of_mealOffsetBoundIsSmallerThanWakeSleepBound() {
        // Assert — the design constraint: meal range must be strictly smaller than the wake/sleep
        // range so the village retains a shared rough meal window even after jitter.
        assertTrue(MEAL_RANGE < WAKE_SLEEP_RANGE,
                "Meal range should be smaller than wake/sleep range to preserve village-wide overlap.");
    }

    @Test
    void of_spreadAcrossManySeedsCoversMultipleValues() {
        // Arrange — sample a wide range of seeds and verify the wake/sleep offsets aren't all the same
        int minSeen = Integer.MAX_VALUE;
        int maxSeen = Integer.MIN_VALUE;

        // Act
        for (long seed = 0; seed < 200; seed++) {
            int offset = Chronotype.of(seed).wakeSleepOffsetTicks();
            if (offset < minSeen) {
                minSeen = offset;
            }
            if (offset > maxSeen) {
                maxSeen = offset;
            }
        }

        // Assert — 200 samples should cover more than a trivial range
        assertTrue(maxSeen - minSeen > WAKE_SLEEP_RANGE,
                "Expected spread > RANGE but min=" + minSeen + ", max=" + maxSeen);
    }

}
