package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.planning.Chronotype;
import dev.breezes.settlements.domain.ai.planning.IWakeTickResolver;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import dev.breezes.settlements.domain.ai.schedule.ScheduleProfile;
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
                               @Nonnull PlanDayType dayType,
                               long seed) {
        int wakeTick = profile.defaultWakeTick();
        if (dayType == PlanDayType.REST_DAY) {
            // Rest days unconditionally shift wake later so villagers sleep in — the chronotype
            // then layered on top means rest mornings don't re-synchronize the village either.
            wakeTick += GameTicks.hours(1).getTicksAsInt();
        }
        // Chronotype gives each villager a stable early-bird / night-owl personality derived from
        // their UUID seed. Genetics no longer affect wake timing — WIL still shapes work-end.
        wakeTick += Chronotype.of(seed).wakeSleepOffsetTicks();
        return Math.floorMod(wakeTick, TICKS_PER_DAY);
    }

}
