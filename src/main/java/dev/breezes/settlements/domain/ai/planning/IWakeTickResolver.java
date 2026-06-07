package dev.breezes.settlements.domain.ai.planning;

import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import dev.breezes.settlements.domain.ai.schedule.ScheduleProfile;

import javax.annotation.Nonnull;

public interface IWakeTickResolver {

    /**
     * Resolves the wake tick within an authored day for the given inputs.
     * Returns a tick in {@code [0, TICKS_PER_DAY)}.
     *
     * @param seed stable per-villager seed (e.g. UUID bits XOR'd) used to derive the chronotype
     *             offset deterministically — must NOT be live RNG so that repeated calls for the
     *             same villager always produce the same wake tick.
     */
    int resolveWakeTick(@Nonnull ScheduleProfile profile,
                        @Nonnull PlanDayType dayType,
                        long seed);

}
