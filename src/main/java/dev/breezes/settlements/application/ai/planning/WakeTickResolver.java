package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.planning.IWakeTickResolver;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import dev.breezes.settlements.domain.ai.schedule.ScheduleProfile;
import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;
import dev.breezes.settlements.domain.time.GameTicks;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import static dev.breezes.settlements.domain.time.TimeOfDay.TICKS_PER_DAY;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class WakeTickResolver implements IWakeTickResolver {

    @Override
    public int resolveWakeTick(@Nonnull ScheduleProfile profile,
                               @Nonnull GeneticsProfile genetics,
                               @Nonnull PlanDayType dayType) {
        int wakeTick = profile.defaultWakeTick();
        if (dayType == PlanDayType.REST_DAY) {
            wakeTick += GameTicks.hours(1).getTicksAsInt();
        }
        wakeTick += (int) ((0.5 - genetics.getGeneValue(GeneType.CONSTITUTION))
                * GameTicks.minutes(60).getTicksAsInt());
        return Math.floorMod(wakeTick, TICKS_PER_DAY);
    }

}
