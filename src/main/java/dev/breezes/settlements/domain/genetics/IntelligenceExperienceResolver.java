package dev.breezes.settlements.domain.genetics;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class IntelligenceExperienceResolver {

    private static final double INTELLIGENCE_IMPACT = 0.5;
    private static final double MIN_MULTIPLIER = 0.25;

    public static double resolveMultiplier(double intelligence) {
        return Math.max(MIN_MULTIPLIER, GeneticMultiplierResolver.centeredMultiplier(intelligence, INTELLIGENCE_IMPACT));
    }

}
