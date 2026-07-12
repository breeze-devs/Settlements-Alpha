package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.catalog.BehaviorCategory;
import dev.breezes.settlements.domain.ai.catalog.BehaviorPlanningMetadata;
import dev.breezes.settlements.domain.ai.catalog.WeightedBehavior;
import dev.breezes.settlements.domain.ai.catalog.WorkIntensity;
import dev.breezes.settlements.domain.ai.planning.PlanGenerationContext;
import dev.breezes.settlements.domain.ai.schedule.RestDayPolicy;
import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Pool derivation and weight-multiplier policy shared by every plan-composition consumer.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlannerPolicy {

    // Down-weight factor for behaviors whose declared opportunity requirements are not currently met
    public static final double LOW_OPPORTUNITY_MULTIPLIER = 0.2;

    // Base multiplier for bands that carry no day-type policy weighting
    public static final EffectiveWeightMultiplier DEFAULT_EFFECTIVE_WEIGHT_MULTIPLIER = behavior -> 1.0D;

    @FunctionalInterface
    public interface EffectiveWeightMultiplier {

        double apply(WeightedBehavior behavior);

        /**
         * Composes this multiplier with another, producing a multiplier whose value is the product.
         */
        default EffectiveWeightMultiplier andThen(EffectiveWeightMultiplier other) {
            return behavior -> this.apply(behavior) * other.apply(behavior);
        }

    }

    public static double restDayMultiplier(BehaviorPlanningMetadata behavior, RestDayPolicy policy) {
        if (behavior.getCategory() == BehaviorCategory.SOCIAL) {
            return policy.socialMultiplier();
        }
        if (behavior.getCategory() == BehaviorCategory.SELF_CARE) {
            return policy.selfCareMultiplier();
        }
        if (behavior.getCategory() == BehaviorCategory.LEISURE) {
            return policy.leisureMultiplier();
        }
        if (behavior.getCategory() == BehaviorCategory.WORK && behavior.getIntensity() == WorkIntensity.LIGHT) {
            return policy.lightWorkMultiplier();
        }
        if (behavior.getCategory() == BehaviorCategory.WORK && behavior.getIntensity() == WorkIntensity.HEAVY) {
            return policy.heavyWorkMultiplier();
        }
        return 0.0D;
    }

    /**
     * Returns a multiplier that applies {@link #LOW_OPPORTUNITY_MULTIPLIER} to behaviors listed
     * in the context's lacking set, and 1.0 to all others.
     */
    public static EffectiveWeightMultiplier opportunityMultiplier(PlanGenerationContext context) {
        return behavior -> context.behaviorsLackingOpportunity().contains(behavior.key())
                ? LOW_OPPORTUNITY_MULTIPLIER
                : 1.0D;
    }

    /**
     * Encapsulates behavior filtering and key resolution for a single plan generation call.
     * All methods are pure queries over the provided behavior list — no state is mutated.
     */
    public record PlannerPalette(List<WeightedBehavior> availableBehaviors) {

        public List<WeightedBehavior> workBehaviors() {
            return this.availableBehaviors.stream()
                    .filter(behavior -> behavior.descriptor().getCategory() == BehaviorCategory.WORK)
                    .toList();
        }

        /**
         * Afternoon candidates ordered by social preference: social-first for high-CHA villagers,
         * social-last for low-CHA — then light work, then leisure fills the remainder.
         */
        public List<WeightedBehavior> afternoonCandidates(GeneticsProfile genetics) {
            boolean socialPreference = genetics.getGeneValue(GeneType.CHARISMA) >= 0.45;
            List<WeightedBehavior> social = this.byCategory(BehaviorCategory.SOCIAL);
            List<WeightedBehavior> selfCare = this.byCategory(BehaviorCategory.SELF_CARE);
            List<WeightedBehavior> leisure = this.byCategory(BehaviorCategory.LEISURE);
            List<WeightedBehavior> lightWork = this.workBehaviors().stream()
                    .filter(behavior -> behavior.descriptor().getIntensity() == WorkIntensity.LIGHT)
                    .toList();

            List<WeightedBehavior> candidates = new ArrayList<>();
            if (socialPreference) {
                candidates.addAll(social);
            }

            candidates.addAll(lightWork);
            candidates.addAll(selfCare);
            candidates.addAll(leisure);

            if (!socialPreference) {
                candidates.addAll(social);
            }

            return candidates;
        }

        /**
         * Evening candidates: the after-dinner wind-down pool — social, leisure, self-care, no work
         */
        public List<WeightedBehavior> eveningCandidates() {
            List<WeightedBehavior> candidates = new ArrayList<>();
            candidates.addAll(this.byCategory(BehaviorCategory.SOCIAL));
            candidates.addAll(this.byCategory(BehaviorCategory.LEISURE));
            candidates.addAll(this.byCategory(BehaviorCategory.SELF_CARE));
            return candidates;
        }

        private List<WeightedBehavior> byCategory(BehaviorCategory category) {
            return this.availableBehaviors.stream()
                    .filter(behavior -> behavior.descriptor().getCategory() == category)
                    .toList();
        }

    }

}
