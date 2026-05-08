package dev.breezes.settlements.domain.time;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Game-time duration expressed in Minecraft ticks.
 * <p>
 * Unlike {@link ClockTicks}, which measures real-world elapsed ticks (20 per real second),
 * this class expresses durations in terms of in-game clock time, where one game hour
 * equals 1,000 ticks (one Minecraft day = 24,000 ticks = 20 real minutes).
 * <p>
 * Use this class whenever working with {@link TimeOfDay} values, {@code PlanSlot.startTick},
 * or any other quantity that lives on the in-game clock rather than real elapsed time.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GameTicks {

    /**
     * One Minecraft game hour in ticks. A full day is 24 game hours = 24,000 ticks.
     */
    public static final int TICKS_PER_GAME_HOUR = 1_000;

    private static final double TICKS_PER_GAME_MINUTE = TICKS_PER_GAME_HOUR / 60.0;

    private final int ticks;

    public static GameTicks of(int ticks) {
        return new GameTicks(ticks);
    }

    /**
     * Duration expressed as game hours (1 game hour = 1,000 ticks).
     */
    public static GameTicks hours(double hours) {
        return of((int) (hours * TICKS_PER_GAME_HOUR));
    }

    /**
     * Duration expressed as game minutes (1 game minute ≈ 16.67 ticks; fractional ticks are truncated).
     */
    public static GameTicks minutes(double minutes) {
        return of((int) (minutes * TICKS_PER_GAME_MINUTE));
    }

    public int getTicksAsInt() {
        return this.ticks;
    }

    public int getAsGameMinutes() {
        return (int) Math.round(this.ticks / TICKS_PER_GAME_MINUTE);
    }

    public int getAsGameHours() {
        return this.ticks / TICKS_PER_GAME_HOUR;
    }

    public ClockTicks asClockTicks() {
        return ClockTicks.of(this.getTicksAsInt());
    }

}
