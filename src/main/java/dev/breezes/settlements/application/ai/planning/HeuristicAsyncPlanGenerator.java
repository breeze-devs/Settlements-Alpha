package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.ai.planning.IAsyncPlanGenerator;
import dev.breezes.settlements.domain.ai.planning.IPlanGenerator;
import dev.breezes.settlements.domain.ai.planning.PlanGenerationContext;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Exercises the async orchestration path while keeping the current heuristic planner as the source
 * of truth until a slower generator, such as an LLM planner, is introduced.
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class HeuristicAsyncPlanGenerator implements IAsyncPlanGenerator {

    private final IPlanGenerator planGenerator;
    private final ExecutorService planGenerationExecutor;

    @Override
    public CompletableFuture<DayPlan> generateAsync(@Nonnull PlanGenerationContext context) {
        return CompletableFuture.supplyAsync(() -> this.planGenerator.generate(context), this.planGenerationExecutor);
    }

}
