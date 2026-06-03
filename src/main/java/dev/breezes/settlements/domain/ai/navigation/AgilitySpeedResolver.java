package dev.breezes.settlements.domain.ai.navigation;

import dev.breezes.settlements.domain.genetics.GeneticMultiplierResolver;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class AgilitySpeedResolver {

    private static final float AGILITY_IMPACT = 0.2F;
    private static final float MIN_MODIFIER = 0.1F;

    public static float resolve(@Nonnull NavigationType type, double agility) {
        float multiplier = (float) GeneticMultiplierResolver.centeredMultiplier(agility, AGILITY_IMPACT);
        return Math.max(MIN_MODIFIER, type.getBaseModifier() * multiplier);
    }

}
