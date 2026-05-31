package dev.breezes.settlements.domain.ai.navigation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgilitySpeedResolverTest {

    private static final float DELTA = 0.0001F;

    @Test
    void resolve_returnsBaseModifierWhenAgilityIsCentered() {
        float result = AgilitySpeedResolver.resolve(NavigationType.WALK, 0.5);

        assertEquals(0.5F, result, DELTA);
    }

    @Test
    void resolve_appliesPositiveAgilityImpactAtMaximumAgility() {
        float result = AgilitySpeedResolver.resolve(NavigationType.RUN, 1.0);

        assertEquals(0.84F, result, DELTA);
    }

    @Test
    void resolve_appliesNegativeAgilityImpactAtMinimumAgility() {
        float result = AgilitySpeedResolver.resolve(NavigationType.SPRINT, 0.0);

        assertEquals(0.72F, result, DELTA);
    }

    @Test
    void resolve_neverReturnsLessThanSafetyFloor() {
        float result = AgilitySpeedResolver.resolve(NavigationType.STROLL, -10.0);

        assertEquals(0.1F, result, DELTA);
    }

}
