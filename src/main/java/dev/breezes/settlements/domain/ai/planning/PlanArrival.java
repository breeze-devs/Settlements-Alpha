package dev.breezes.settlements.domain.ai.planning;

/**
 * One producer's completed {@link DayPlan}, tagged with who authored it.
 */
public record PlanArrival(DayPlan plan, PlanAuthor author) {

    /**
     * The calendar day this arrival's plan was authored for — the identity arbitration keys on.
     */
    public long targetDay() {
        return this.plan.getCalendarDay();
    }

}
