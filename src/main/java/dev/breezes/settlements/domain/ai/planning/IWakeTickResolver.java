package dev.breezes.settlements.domain.ai.planning;

import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import dev.breezes.settlements.domain.ai.schedule.ScheduleProfile;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;

import javax.annotation.Nonnull;

public interface IWakeTickResolver {

    /**
     * Resolves the wake tick within an authored day for the given inputs.
     * Returns a tick in {@code [0, TICKS_PER_DAY)}.
     */
    int resolveWakeTick(@Nonnull ScheduleProfile profile,
                        @Nonnull GeneticsProfile genetics,
                        @Nonnull PlanDayType dayType);

}
