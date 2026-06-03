package dev.breezes.settlements.domain.genetics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeneticMultiplierResolverTest {

    private static final double DELTA = 0.0001;

    @Test
    void centeredMultiplier_returnsNegativeImpactAtMinimumGene() {
        double result = GeneticMultiplierResolver.centeredMultiplier(0.0, 0.5);

        assertEquals(0.5, result, DELTA);
    }

    @Test
    void centeredMultiplier_returnsNeutralMultiplierAtCenteredGene() {
        double result = GeneticMultiplierResolver.centeredMultiplier(0.5, 0.5);

        assertEquals(1.0, result, DELTA);
    }

    @Test
    void centeredMultiplier_returnsPositiveImpactAtMaximumGene() {
        double result = GeneticMultiplierResolver.centeredMultiplier(1.0, 0.5);

        assertEquals(1.5, result, DELTA);
    }

}
