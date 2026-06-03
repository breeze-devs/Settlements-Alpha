package dev.breezes.settlements.domain.genetics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntelligenceExperienceResolverTest {

    private static final double DELTA = 0.0001;

    @Test
    void resolveMultiplier_returnsMinimumGeneMultiplier() {
        double result = IntelligenceExperienceResolver.resolveMultiplier(0.0);

        assertEquals(0.5, result, DELTA);
    }

    @Test
    void resolveMultiplier_returnsMeanPopulationMultiplier() {
        double result = IntelligenceExperienceResolver.resolveMultiplier(0.4);

        assertEquals(0.9, result, DELTA);
    }

    @Test
    void resolveMultiplier_returnsCenteredMultiplier() {
        double result = IntelligenceExperienceResolver.resolveMultiplier(0.5);

        assertEquals(1.0, result, DELTA);
    }

    @Test
    void resolveMultiplier_returnsMaximumGeneMultiplier() {
        double result = IntelligenceExperienceResolver.resolveMultiplier(1.0);

        assertEquals(1.5, result, DELTA);
    }

    @Test
    void resolveMultiplier_neverReturnsLessThanSafetyFloor() {
        double result = IntelligenceExperienceResolver.resolveMultiplier(-10.0);

        assertEquals(0.25, result, DELTA);
    }

}
