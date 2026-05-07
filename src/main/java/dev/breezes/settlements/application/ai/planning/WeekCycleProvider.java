package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.schedule.IWeekCycleProvider;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;

import javax.inject.Inject;

/**
 * Default village-wide work rhythm: two work days followed by one rest day.
 */
public class WeekCycleProvider implements IWeekCycleProvider {

    private static final int REST_DAY_CYCLE_POSITION = 2;
    private static final int CYCLE_LENGTH_DAYS = 3;

    @Inject
    public WeekCycleProvider() {
    }

    @Override
    public PlanDayType getDayType(long gameDay) {
        return Math.floorMod(gameDay, CYCLE_LENGTH_DAYS) == REST_DAY_CYCLE_POSITION
                ? PlanDayType.REST_DAY
                : PlanDayType.WORK_DAY;
    }

}
