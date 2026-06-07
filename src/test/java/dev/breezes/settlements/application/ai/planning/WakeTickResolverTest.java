package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import dev.breezes.settlements.domain.ai.schedule.ScheduleProfile;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.time.GameTicks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static dev.breezes.settlements.domain.time.TimeOfDay.TICKS_PER_DAY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WakeTickResolverTest {

    private final WakeTickResolver resolver = new WakeTickResolver();

    private static final ScheduleProfile FARMER_PROFILE = ScheduleProfile.builder()
            .profession(VillagerProfessionKey.FARMER)
            .defaultWakeTick(6_400)
            .defaultSleepTick(13_000)
            .workStartTick(7_000)
            .workEndTick(12_000)
            .scheduleDescription("test")
            .build();

    @Test
    void resolveWakeTick_sameSeedAlwaysProducesSameResult() {
        // Arrange
        long seed = 0xDEADBEEF_CAFEBABEL;

        // Act
        int first = resolver.resolveWakeTick(FARMER_PROFILE, PlanDayType.WORK_DAY, seed);
        int second = resolver.resolveWakeTick(FARMER_PROFILE, PlanDayType.WORK_DAY, seed);
        int third = resolver.resolveWakeTick(FARMER_PROFILE, PlanDayType.WORK_DAY, seed);

        // Assert — determinism is load-bearing: PlanRunner and the generator must always agree
        assertEquals(first, second);
        assertEquals(second, third);
    }

    @Test
    void resolveWakeTick_resultIsAlwaysInValidDayRange() {
        // Arrange + Act + Assert
        for (long seed = 0; seed < 200; seed++) {
            int wakeTick = resolver.resolveWakeTick(FARMER_PROFILE, PlanDayType.WORK_DAY, seed);
            assertTrue(wakeTick >= 0 && wakeTick < TICKS_PER_DAY,
                    "Expected wake tick in [0, TICKS_PER_DAY) but got " + wakeTick + " for seed=" + seed);
        }
    }

    @Test
    void resolveWakeTick_restDayAddsOneHourRelativeToWorkDay() {
        // Arrange — same seed, both day types
        long seed = 42L;

        // Act
        int workDayWake = resolver.resolveWakeTick(FARMER_PROFILE, PlanDayType.WORK_DAY, seed);
        int restDayWake = resolver.resolveWakeTick(FARMER_PROFILE, PlanDayType.REST_DAY, seed);

        // Assert — rest day should be exactly one in-game hour later (modular arithmetic respected)
        int expectedRestDayWake = Math.floorMod(workDayWake + GameTicks.hours(1).getTicksAsInt(), TICKS_PER_DAY);
        assertEquals(expectedRestDayWake, restDayWake);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, 99L, Long.MAX_VALUE, Long.MIN_VALUE})
    void resolveWakeTick_neverDependsOnGenetics_alwaysInRange(long seed) {
        // Arrange + Act
        int wakeTick = resolver.resolveWakeTick(FARMER_PROFILE, PlanDayType.WORK_DAY, seed);

        // Assert — no genetics parameter exists; result is within bounds
        assertTrue(wakeTick >= 0 && wakeTick < TICKS_PER_DAY);
    }

    @Test
    void resolveWakeTick_differentSeedsProduceSpreadAcrossRange() {
        // Arrange — sample enough seeds to expect meaningful spread
        int minSeen = Integer.MAX_VALUE;
        int maxSeen = Integer.MIN_VALUE;

        // Act
        for (long seed = 0; seed < 300; seed++) {
            int wake = resolver.resolveWakeTick(FARMER_PROFILE, PlanDayType.WORK_DAY, seed);
            if (wake < minSeen) {
                minSeen = wake;
            }
            if (wake > maxSeen) {
                maxSeen = wake;
            }
        }

        // Assert — with 300 samples the spread should cover more than 500 ticks (~30 game-minutes)
        int spread = Math.floorMod(maxSeen - minSeen, TICKS_PER_DAY);
        assertTrue(spread > 500,
                "Expected wake times to spread across >500 ticks but spread was only " + spread);
    }

    @Test
    void resolveWakeTick_wrapsWithinSingleDay() {
        // Arrange — a profile whose default is near midnight, so the chronotype might wrap it
        ScheduleProfile nearMidnightProfile = ScheduleProfile.builder()
                .profession(VillagerProfessionKey.FARMER)
                .defaultWakeTick(TICKS_PER_DAY - GameTicks.minutes(30).getTicksAsInt())
                .defaultSleepTick(13_000)
                .workStartTick(7_000)
                .workEndTick(12_000)
                .scheduleDescription("test")
                .build();

        // Act + Assert — result must always be in [0, TICKS_PER_DAY)
        for (long seed = 0; seed < 100; seed++) {
            int wake = resolver.resolveWakeTick(nearMidnightProfile, PlanDayType.WORK_DAY, seed);
            assertTrue(wake >= 0 && wake < TICKS_PER_DAY,
                    "Wake tick out of bounds for seed=" + seed + ": " + wake);
        }
    }

    @Test
    void resolveWakeTick_twoDistinctSeedsGenerallyDiffer() {
        // Arrange — use two very different seeds
        long seedA = 12345L;
        long seedB = 98765L;

        // Act
        int wakeA = resolver.resolveWakeTick(FARMER_PROFILE, PlanDayType.WORK_DAY, seedA);
        int wakeB = resolver.resolveWakeTick(FARMER_PROFILE, PlanDayType.WORK_DAY, seedB);

        // Assert — two different seeds should (with overwhelming probability) yield different results
        assertNotEquals(wakeA, wakeB, "Two distinct seeds produced identical wake ticks — check chronotype seeding.");
    }

}
