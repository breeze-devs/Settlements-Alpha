package dev.breezes.settlements.domain.generation.model.profile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScaleTierTest {

    @ParameterizedTest
    @EnumSource(ScaleTier.class)
    void forHeadcount_reachesATier_atItsMinimumPopulation(ScaleTier tier) {
        // Act
        ScaleTier observed = ScaleTier.forHeadcount(tier.minPopulation());

        // Assert
        assertEquals(tier, observed);
    }

    @Test
    void forHeadcount_staysAVillage_justBelowTownsMinimum() {
        // Act
        ScaleTier observed = ScaleTier.forHeadcount(ScaleTier.TOWN.minPopulation() - 1);

        // Assert
        assertEquals(ScaleTier.VILLAGE, observed);
    }

    @Test
    void forHeadcount_staysAHamlet_justBelowVillagesMinimum() {
        // Act
        ScaleTier observed = ScaleTier.forHeadcount(ScaleTier.VILLAGE.minPopulation() - 1);

        // Assert
        assertEquals(ScaleTier.HAMLET, observed);
    }

    @Test
    void forHeadcount_isAHamlet_belowHamletsMinimum() {
        // Act
        ScaleTier observed = ScaleTier.forHeadcount(ScaleTier.HAMLET.minPopulation() - 1);

        // Assert
        assertEquals(ScaleTier.HAMLET, observed);
    }

    @Test
    void forHeadcount_isAHamlet_forNoVillagers() {
        // Act
        ScaleTier observed = ScaleTier.forHeadcount(0);

        // Assert
        assertEquals(ScaleTier.HAMLET, observed);
    }

    @Test
    void forHeadcount_staysATown_pastTownsMaximum() {
        // Act — a founded settlement's headcount is not bounded by any tier's range
        ScaleTier observed = ScaleTier.forHeadcount(ScaleTier.TOWN.maxPopulation() + 1);

        // Assert
        assertEquals(ScaleTier.TOWN, observed);
    }

    @Test
    void forHeadcount_rejectsANegativeHeadcount() {
        // Act and Assert
        assertThrows(IllegalArgumentException.class, () -> ScaleTier.forHeadcount(-1));
    }

}
