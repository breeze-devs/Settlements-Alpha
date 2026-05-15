package dev.breezes.settlements.domain.ai.planning;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

/**
 * Generates plans without requiring the server thread to wait for the result.
 * <p>
 * Implementations must treat {@link PlanGenerationContext} as an immutable snapshot and must not
 * mutate live Minecraft state from worker threads.
 */
public interface IAsyncPlanGenerator {

    CompletableFuture<DayPlan> generateAsync(@Nonnull PlanGenerationContext context);

}
