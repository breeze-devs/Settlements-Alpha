package dev.breezes.settlements.domain.ai.schedule;

/**
 * Determines whether a given absolute game day is a work day or a rest day.
 */
public interface IWeekCycleProvider {

    PlanDayType getDayType(long gameDay);

}
