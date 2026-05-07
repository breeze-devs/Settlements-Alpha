package dev.breezes.settlements.domain.ai.planning;

/**
 * Generates a {@link DayPlan} from the current villager context.
 */
public interface IPlanGenerator {

    DayPlan generate(PlanGenerationContext context);

}
