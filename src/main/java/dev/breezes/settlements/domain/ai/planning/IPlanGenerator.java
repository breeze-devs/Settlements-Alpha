package dev.breezes.settlements.domain.ai.planning;

/**
 * Generates a {@link DayPlan} from the current villager context.
 */
public interface IPlanGenerator {

    /**
     * Equivalent to {@link #generate(PlanGenerationContext, PlanIntent)} with {@link PlanIntent#empty()}.
     */
    default DayPlan generate(PlanGenerationContext context) {
        return this.generate(context, PlanIntent.empty());
    }

    DayPlan generate(PlanGenerationContext context, PlanIntent intent);

}
