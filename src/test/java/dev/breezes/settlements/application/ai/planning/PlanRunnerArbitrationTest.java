package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.ai.planning.DayPlanSchedule;
import dev.breezes.settlements.domain.ai.planning.PlanArrival;
import dev.breezes.settlements.domain.ai.planning.PlanAuthor;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class PlanRunnerArbitrationTest {

    @Test
    void arbitrate_nullStaged_acceptsIncoming() {
        // Arrange
        PlanArrival incoming = arrival(5L, PlanAuthor.HEURISTIC);

        // Act
        PlanArrival result = PlanRunner.arbitrate(null, incoming, 5L);

        // Assert
        assertSame(incoming, result);
    }

    @Test
    void arbitrate_llmSupersedesStagedHeuristicOnSameDay() {
        // Arrange
        PlanArrival staged = arrival(5L, PlanAuthor.HEURISTIC);
        PlanArrival incoming = arrival(5L, PlanAuthor.LLM);

        // Act
        PlanArrival result = PlanRunner.arbitrate(staged, incoming, 5L);

        // Assert
        assertSame(incoming, result);
    }

    @Test
    void arbitrate_heuristicDoesNotReplaceStagedLlmOnSameDay() {
        // Arrange
        PlanArrival staged = arrival(5L, PlanAuthor.LLM);
        PlanArrival incoming = arrival(5L, PlanAuthor.HEURISTIC);

        // Act
        PlanArrival result = PlanRunner.arbitrate(staged, incoming, 5L);

        // Assert
        assertSame(staged, result);
    }

    @Test
    void arbitrate_sameAuthorNewestGenerationWins() {
        // Arrange — two heuristic arrivals for the same day; the later one supersedes.
        PlanArrival staged = arrival(5L, PlanAuthor.HEURISTIC);
        PlanArrival incoming = arrival(5L, PlanAuthor.HEURISTIC);

        // Act
        PlanArrival result = PlanRunner.arbitrate(staged, incoming, 5L);

        // Assert
        assertSame(incoming, result);
    }

    @Test
    void arbitrate_fresherTargetDayWinsRegardlessOfAuthor() {
        // Arrange — a heuristic plan for a later day beats a staged LLM plan for an earlier day.
        PlanArrival staged = arrival(6L, PlanAuthor.LLM);
        PlanArrival incoming = arrival(7L, PlanAuthor.HEURISTIC);

        // Act
        PlanArrival result = PlanRunner.arbitrate(staged, incoming, 5L);

        // Assert
        assertSame(incoming, result);
    }

    @Test
    void arbitrate_staleIncomingIsDropped() {
        // Arrange — incoming targets an already-past day; the staged arrival is kept.
        PlanArrival staged = arrival(5L, PlanAuthor.HEURISTIC);
        PlanArrival incoming = arrival(4L, PlanAuthor.LLM);

        // Act
        PlanArrival result = PlanRunner.arbitrate(staged, incoming, 5L);

        // Assert
        assertSame(staged, result);
    }

    @Test
    void arbitrate_staleIncomingWithNullStagedReturnsNull() {
        // Arrange — nothing staged and the incoming arrival is already past; totality means null out.
        PlanArrival incoming = arrival(4L, PlanAuthor.LLM);

        // Act
        PlanArrival result = PlanRunner.arbitrate(null, incoming, 5L);

        // Assert
        assertNull(result);
    }

    private static PlanArrival arrival(long calendarDay, PlanAuthor author) {
        DayPlan plan = DayPlan.builder()
                .dayType(PlanDayType.WORK_DAY)
                .calendarDay(calendarDay)
                .schedule(schedule())
                .build();
        return new PlanArrival(plan, author);
    }

    private static DayPlanSchedule schedule() {
        return DayPlanSchedule.builder()
                .wakeTick(0)
                .bedtimeTick(12_000)
                .build();
    }

}
