package dev.breezes.settlements.domain.genetics;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneticsProfileTest {

    @Test
    void crossover_keepsChildGeneValuesWithinValidBounds() {
        // Arrange
        GeneticsProfile firstParent = profileWithValue(0.0D);
        GeneticsProfile secondParent = profileWithValue(1.0D);
        RandomSource random = RandomSource.create(12345L);

        // Act
        GeneticsProfile child = firstParent.crossover(secondParent, random);

        // Assert
        for (Gene gene : child.getAllGenes().values()) {
            assertTrue(gene.value() >= 0.0D && gene.value() <= 1.0D);
        }
    }

    private static GeneticsProfile profileWithValue(double value) {
        Map<GeneType, Gene> genes = new EnumMap<>(GeneType.class);
        for (GeneType type : GeneType.VALUES) {
            genes.put(type, new Gene(value));
        }
        return new GeneticsProfile(genes);
    }

}
