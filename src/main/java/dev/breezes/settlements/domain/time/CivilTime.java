package dev.breezes.settlements.domain.time;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Locale;

/**
 * Conversion between Minecraft-space ticks (day begins at tick 0 == 06:00 dawn) and
 * civil-space ticks (day begins at tick 0 == 00:00 midnight).
 * <p>
 * {@link TimeOfDay#getMinecraftTick()} and the raw {@code dayTime}/{@code getDayTime()} values
 * exposed by Minecraft all live on the dawn-anchored clock, which is convenient for the engine
 * but not for calendar-day bookkeeping: pre-dawn hours end up numerically larger than the
 * evening before them. Civil ticks re-anchor the same 24,000-tick cycle onto midnight so that
 * ticks increase monotonically across a real calendar day. This class is the single place that
 * performs the shift so the rest of the codebase never hand-rolls the +/- 6000 offset.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CivilTime {

    /**
     * Distance from dawn (MC tick 0) to midnight (MC tick {@link TimeOfDay#AT_00_00}), i.e. how
     * far civil-space is offset from Minecraft-space.
     */
    private static final int MC_TO_CIVIL_OFFSET = TimeOfDay.TICKS_PER_DAY - TimeOfDay.AT_00_00.getMinecraftTick();

    /**
     * Converts a Minecraft-space tick (0 = dawn) to its civil-space equivalent (0 = midnight).
     * {@code floorMod} keeps the result in {@code [0, 24000)} regardless of how far out of range
     * {@code mcTick} is, mirroring how Minecraft's own dayTime wraps.
     */
    public static int civilFromMcTick(int mcTick) {
        return Math.floorMod(mcTick + MC_TO_CIVIL_OFFSET, TimeOfDay.TICKS_PER_DAY);
    }

    /**
     * Converts a civil-space tick (0 = midnight) back to its Minecraft-space equivalent (0 = dawn).
     * Inverse of {@link #civilFromMcTick(int)}.
     */
    public static int mcTickFromCivil(int civilTick) {
        return Math.floorMod(civilTick - MC_TO_CIVIL_OFFSET, TimeOfDay.TICKS_PER_DAY);
    }

    /**
     * Converts an absolute, unbounded {@code dayTime} (Minecraft's {@code Level.getDayTime()},
     * which accumulates across all elapsed days rather than wrapping) to a civil-space tick in
     * {@code [0, 24000)}. The intermediate {@code floorMod} strips off the elapsed-days portion
     * before the dawn-to-midnight shift is applied.
     */
    public static int civilFromDayTime(long dayTime) {
        return civilFromMcTick(Math.floorMod(dayTime, TimeOfDay.TICKS_PER_DAY));
    }

    /**
     * Renders a civil-space tick as an "HH:MM" clock reading. The input is a tick within one day,
     * in [0, 24000); everything that stores or derives a civil tick already holds one.
     * <p>
     * A tick outside that range renders as itself rather than being wrapped into range. Wrapping
     * would turn corrupt state into a plausible time of day, and the readings this formats are
     * exactly what a reader would use to notice that corruption.
     */
    public static String formatClock(long civilTick) {
        if (civilTick < 0 || civilTick >= TimeOfDay.TICKS_PER_DAY) {
            return "??:?? (" + civilTick + ")";
        }

        long hour = civilTick / 1000;
        long minute = (civilTick % 1000) * 60 / 1000;
        return String.format(Locale.ROOT, "%02d:%02d", hour, minute);
    }

}
