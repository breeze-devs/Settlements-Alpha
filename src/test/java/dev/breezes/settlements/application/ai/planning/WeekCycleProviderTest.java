package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeekCycleProviderTest {

    private final WeekCycleProvider provider = new WeekCycleProvider();

    @Test
    void getDayType_returnsWorkWorkRestCycle() {
        assertEquals(PlanDayType.WORK_DAY, this.provider.getDayType(0));
        assertEquals(PlanDayType.WORK_DAY, this.provider.getDayType(1));
        assertEquals(PlanDayType.REST_DAY, this.provider.getDayType(2));
        assertEquals(PlanDayType.WORK_DAY, this.provider.getDayType(3));
        assertEquals(PlanDayType.WORK_DAY, this.provider.getDayType(4));
        assertEquals(PlanDayType.REST_DAY, this.provider.getDayType(5));
    }

    @Test
    void getDayType_handlesNegativeInputDeterministically() {
        assertEquals(PlanDayType.REST_DAY, this.provider.getDayType(-1));
        assertEquals(PlanDayType.WORK_DAY, this.provider.getDayType(-2));
        assertEquals(PlanDayType.WORK_DAY, this.provider.getDayType(-3));
    }

}
