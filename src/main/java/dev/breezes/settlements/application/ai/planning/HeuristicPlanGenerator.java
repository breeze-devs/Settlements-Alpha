package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.ai.planning.IPlanGenerator;
import dev.breezes.settlements.domain.ai.planning.PlanGenerationContext;
import dev.breezes.settlements.domain.ai.planning.PlanIntent;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.inject.Inject;

/**
 * Produces a villager's daily plan using deterministic heuristics.
 * <p>
 * A thin {@link IPlanGenerator} adapter: every stage (frame, meal anchors, band derivation,
 * gap-fill) lives in {@link DayPlanComposer}. The single-arg {@link IPlanGenerator#generate}
 * default forwards here with {@link PlanIntent#empty()}, which is what selects the composer's
 * pure-heuristic fill policy — pack each band's own derived pool with coverage-first
 * weighted-random selection. A non-empty intent (e.g. carried-forward pins) flows straight
 * through to the composer unchanged.
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class HeuristicPlanGenerator implements IPlanGenerator {

    private final DayPlanComposer composer;

    @Override
    public DayPlan generate(PlanGenerationContext context, PlanIntent intent) {
        return this.composer.compose(context, intent);
    }

}
