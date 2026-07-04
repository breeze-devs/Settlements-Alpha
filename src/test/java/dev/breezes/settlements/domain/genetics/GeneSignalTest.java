package dev.breezes.settlements.domain.genetics;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeneSignalTest {

    private static final double DELTA = 0.0001;

    @Test
    void allFrom_returnsOneSignalPerGeneInDeclaredOrder() {
        // Arrange
        GeneticsProfile genetics = profileWithValue(0.5);

        // Act
        List<GeneSignal> signals = GeneSignal.allFrom(genetics);

        // Assert
        assertEquals(GeneType.VALUES.length, signals.size());
        for (int i = 0; i < GeneType.VALUES.length; i++) {
            assertEquals(GeneType.VALUES[i], signals.get(i).dimension());
        }
    }

    @Test
    void allFrom_carriesTheRawGeneValueUnmodified() {
        // Arrange
        Map<GeneType, Gene> genes = new EnumMap<>(GeneType.class);
        genes.put(GeneType.STRENGTH, new Gene(0.84));
        genes.put(GeneType.CONSTITUTION, new Gene(0.1));
        genes.put(GeneType.AGILITY, new Gene(0.4));
        genes.put(GeneType.INTELLIGENCE, new Gene(0.6));
        genes.put(GeneType.WILL, new Gene(0.0));
        genes.put(GeneType.CHARISMA, new Gene(1.0));
        GeneticsProfile genetics = new GeneticsProfile(genes);

        // Act
        List<GeneSignal> signals = GeneSignal.allFrom(genetics);

        // Assert
        for (GeneSignal signal : signals) {
            assertEquals(genetics.getGeneValue(signal.dimension()), signal.value(), DELTA);
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
