package dev.breezes.settlements.domain.time;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Specific moments in a Minecraft game day, expressed as game-tick offsets.
 * <p>
 * Minecraft's day begins at tick 0 ({@link #AT_06_00} — dawn). Each in-game hour
 * is 1000 ticks, making a full day 24,000 ticks. Constants cover every half-hour
 * from 00:00 through 23:30 in 24-hour (military) time.
 * <p>
 * Times from 00:00 to 05:30 have tick values above 18 000 because the Minecraft
 * day resets at 06:00 (tick 0), not at midnight.
 */
@AllArgsConstructor
@Getter
public enum TimeOfDay {

    /**
     * Midnight
     */
    AT_00_00(18_000),
    AT_00_30(18_500),
    AT_01_00(19_000),
    AT_01_30(19_500),
    AT_02_00(20_000),
    AT_02_30(20_500),
    AT_03_00(21_000),
    AT_03_30(21_500),
    AT_04_00(22_000),
    AT_04_30(22_500),
    AT_05_00(23_000),
    AT_05_30(23_500),
    /**
     * Start of the Minecraft game day (dawn)
     */
    AT_06_00(0),
    AT_06_30(500),
    AT_07_00(1_000),
    AT_07_30(1_500),
    AT_08_00(2_000),
    AT_08_30(2_500),
    AT_09_00(3_000),
    AT_09_30(3_500),
    AT_10_00(4_000),
    AT_10_30(4_500),
    AT_11_00(5_000),
    AT_11_30(5_500),
    /**
     * Noon
     */
    AT_12_00(6_000),
    AT_12_30(6_500),
    AT_13_00(7_000),
    AT_13_30(7_500),
    AT_14_00(8_000),
    AT_14_30(8_500),
    AT_15_00(9_000),
    AT_15_30(9_500),
    AT_16_00(10_000),
    AT_16_30(10_500),
    AT_17_00(11_000),
    AT_17_30(11_500),
    /**
     * Sunset
     */
    AT_18_00(12_000),
    AT_18_30(12_500),
    AT_19_00(13_000),
    AT_19_30(13_500),
    AT_20_00(14_000),
    AT_20_30(14_500),
    AT_21_00(15_000),
    AT_21_30(15_500),
    AT_22_00(16_000),
    AT_22_30(16_500),
    AT_23_00(17_000),
    AT_23_30(17_500);

    public static final int TICKS_PER_DAY = 24_000;

    private final int tick;

    public static boolean isValidTick(int tick) {
        return tick >= 0 && tick < TICKS_PER_DAY;
    }

}
