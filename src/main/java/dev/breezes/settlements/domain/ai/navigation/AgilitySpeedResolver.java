package dev.breezes.settlements.domain.ai.navigation;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class AgilitySpeedResolver {

    private static final float AGILITY_IMPACT = 0.2F;
    private static final float MIN_MODIFIER = 0.1F;
    private static final float CENTERED_GENE_VALUE = 0.5F;

    public static float resolve(@Nonnull NavigationType type, double agility) {
        float multiplier = 1.0F + (((float) agility - CENTERED_GENE_VALUE) * 2.0F * AGILITY_IMPACT);
        return Math.max(MIN_MODIFIER, type.getBaseModifier() * multiplier);
    }

}
