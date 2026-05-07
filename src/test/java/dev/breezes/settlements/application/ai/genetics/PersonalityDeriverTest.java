package dev.breezes.settlements.application.ai.genetics;

import dev.breezes.settlements.domain.genetics.Gene;
import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PersonalityDeriverTest {

    private final PersonalityDeriver deriver = new PersonalityDeriver();

    @Test
    void derivePersonalitySummary_producesDifferentSummariesForDifferentGenes() {
        GeneticsProfile socialLeader = genetics(0.5, 0.5, 0.5, 0.9, 0.5, 0.9);
        GeneticsProfile solitaryWorker = genetics(0.9, 0.9, 0.5, 0.5, 0.9, 0.1);

        String socialSummary = this.deriver.derivePersonalitySummary(socialLeader);
        String solitarySummary = this.deriver.derivePersonalitySummary(solitaryWorker);

        assertNotEquals(socialSummary, solitarySummary);
    }

    static GeneticsProfile genetics(double strength, double constitution, double agility, double intelligence,
                                    double will, double charisma) {
        Map<GeneType, Gene> genes = new EnumMap<>(GeneType.class);
        genes.put(GeneType.STRENGTH, new Gene(strength));
        genes.put(GeneType.CONSTITUTION, new Gene(constitution));
        genes.put(GeneType.AGILITY, new Gene(agility));
        genes.put(GeneType.INTELLIGENCE, new Gene(intelligence));
        genes.put(GeneType.WILL, new Gene(will));
        genes.put(GeneType.CHARISMA, new Gene(charisma));
        return new GeneticsProfile(genes);
    }

}
