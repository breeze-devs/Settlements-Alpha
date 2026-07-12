package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.catalog.WeightedBehavior;

import java.util.List;

/**
 * One selectable, packable band of a day plan: the candidate pool, its civil-tick bounds and base
 * priority, and the day-type policy multiplier {@link DayPlanComposer} folds in alongside the
 * (always-uniform) opportunity multiplier — which is deliberately NOT part of this type, since it
 * must stay applied identically to every band.
 * <p>
 * {@link #id} is stable across day types — e.g. {@link #MORNING} names the work-day work band and
 * the rest/nitwit morning band alike — and is wire-visible via {@code PlanWindowDTO}, so the LLM
 * overlay can echo it back and map its selections onto the same band.
 * <p>
 * {@link #packFactor} is the over-packing knob: {@link WindowPacker#pack} advances its
 * cursor by {@code round(duration * packFactor)} after a successful emit rather than the full
 * duration, letting a band schedule more (usually-under-estimate) behaviors than its raw width
 * would otherwise fit.
 */
public record PlanBand(String id,
                       int startCivil,
                       int endCivil,
                       int basePriority,
                       List<WeightedBehavior> pool,
                       PlannerPolicy.EffectiveWeightMultiplier baseMultiplier,
                       double packFactor) {

    public static final String MORNING = "MORNING";
    public static final String AFTERNOON = "AFTERNOON";
    public static final String EVENING = "EVENING";

}
