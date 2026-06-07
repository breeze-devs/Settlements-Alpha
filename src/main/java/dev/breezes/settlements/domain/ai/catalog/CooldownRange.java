package dev.breezes.settlements.domain.ai.catalog;

import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.Builder;
import lombok.Getter;

/**
 * Immutable value object representing a behavior's cooldown range in server ticks.
 * <p>
 * Stored as {@link ClockTicks} because behavior cooldowns are anchored to real elapsed time,
 * not the Minecraft in-game clock — the same cadence regardless of day-cycle speed.
 * <p>
 * The {@link #drawTicks()} method is the single call-site for sampling a concrete cooldown
 * value, keeping the random draw testable and encapsulated rather than scattered across
 * the generator.
 */
@Builder
@Getter
public final class CooldownRange {

    private final ClockTicks min;
    private final ClockTicks max;

    /**
     * Constructs a cooldown range from seconds, converting to server ticks via {@link ClockTicks#seconds(double)}.
     * <p>
     * Config values are authored in seconds; converting here keeps the rest of the planning
     * system tick-native without requiring callers to remember the conversion.
     */
    public static CooldownRange ofSeconds(int minSeconds, int maxSeconds) {
        if (minSeconds <= 0 || maxSeconds <= 0 || minSeconds > maxSeconds) {
            throw new IllegalArgumentException("Invalid cooldown range: %d-%d".formatted(minSeconds, maxSeconds));
        }

        return CooldownRange.builder()
                .min(ClockTicks.seconds(minSeconds))
                .max(ClockTicks.seconds(maxSeconds))
                .build();
    }

    /**
     * Draws a random integer from [{@link #min}, {@link #max}] inclusive, returned as a
     * server-tick count ready for use as a cursor offset.
     * <p>
     * Short-circuits to {@code min} when the range is degenerate (min == max) to avoid
     * unnecessary RNG invocation in the common zero-range case.
     */
    public int drawTicks() {
        int minTicks = this.min.getTicksAsInt();
        int maxTicks = this.max.getTicksAsInt();

        if (minTicks == maxTicks) {
            return minTicks;
        }

        return RandomUtil.randomInt(minTicks, maxTicks, true);
    }

}
