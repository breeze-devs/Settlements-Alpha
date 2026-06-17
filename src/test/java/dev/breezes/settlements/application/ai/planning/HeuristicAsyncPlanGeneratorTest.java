package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.ai.planning.DayPlanSchedule;
import dev.breezes.settlements.domain.ai.planning.IPlanGenerator;
import dev.breezes.settlements.domain.ai.planning.PlanGenerationContext;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertSame;

class HeuristicAsyncPlanGeneratorTest {

    @Test
    void generateAsync_returnsFutureWithSyncGeneratorResult() throws Exception {
        // Arrange
        DayPlan expectedPlan = DayPlan.builder()
                .dayType(PlanDayType.WORK_DAY)
                .wakeAtAbsoluteTick(24_000L)
                .schedule(DayPlanSchedule.builder()
                        .wakeTick(0)
                        .bedtimeTick(12_000)
                        .build())
                .build();
        IPlanGenerator syncGenerator = context -> expectedPlan;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        HeuristicAsyncPlanGenerator asyncGenerator = new HeuristicAsyncPlanGenerator(syncGenerator, executor);
        PlanGenerationContext context = PlanGenerationContext.builder()
                .availableBehaviors(List.of())
                .dayType(PlanDayType.WORK_DAY)
                .wakeAtAbsoluteTick(24_000L)
                .build();

        // Act
        CompletableFuture<DayPlan> future = asyncGenerator.generateAsync(context);

        // Assert
        assertSame(expectedPlan, future.get(1, TimeUnit.SECONDS));
        executor.shutdownNow();
    }

}
