package dev.breezes.settlements.domain.genetics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConstitutionHealthResolverTest {

    private static final double DELTA = 0.0001;

    @Test
    void resolveBonus_returnsNegativeBonusAtMinimumConstitution() {
        double result = ConstitutionHealthResolver.resolveBonus(0.0);

        assertEquals(-5.0, result, DELTA);
    }

    @Test
    void resolveBonus_returnsMeanPopulationBonus() {
        double result = ConstitutionHealthResolver.resolveBonus(0.4);

        assertEquals(5.0, result, DELTA);
    }

    @Test
    void resolveBonus_returnsCenteredBonus() {
        double result = ConstitutionHealthResolver.resolveBonus(0.5);

        assertEquals(7.5, result, DELTA);
    }

    @Test
    void resolveBonus_returnsMaximumBonusAtMaximumConstitution() {
        double result = ConstitutionHealthResolver.resolveBonus(1.0);

        assertEquals(20.0, result, DELTA);
    }

}
