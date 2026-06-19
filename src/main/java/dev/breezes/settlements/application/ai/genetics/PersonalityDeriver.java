package dev.breezes.settlements.application.ai.genetics;

import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts raw gene values into stable personality trait tokens for inference payloads
 */
public class PersonalityDeriver {

    private static final double HIGH_THRESHOLD = 0.7;
    private static final double LOW_THRESHOLD = 0.3;

    @Inject
    public PersonalityDeriver() {
    }

    public List<String> derivePersonalityTraits(GeneticsProfile genetics) {
        List<String> traits = new ArrayList<>();

        addTrait(traits, genetics, GeneType.STRENGTH,
                "physically strong", "average build", "physically weak");
        addTrait(traits, genetics, GeneType.CONSTITUTION,
                "resilient and hardy", "typical health", "fragile and sensitive");
        addTrait(traits, genetics, GeneType.AGILITY,
                "quick and alert", "steady-paced", "slow-moving");
        addTrait(traits, genetics, GeneType.INTELLIGENCE,
                "observant and analytical", "practical-minded", "simple-minded");
        addTrait(traits, genetics, GeneType.WILL,
                "stubborn and committed", "agreeable and adaptable", "easily distracted and impressionable");
        addTrait(traits, genetics, GeneType.CHARISMA,
                "charismatic and social", "approachable", "reserved and solitary");

        return traits;
    }

    private static void addTrait(List<String> traits,
                                 GeneticsProfile genetics,
                                 GeneType geneType,
                                 String highTrait,
                                 String midTrait,
                                 String lowTrait) {
        double value = genetics.getGeneValue(geneType);
        if (value >= HIGH_THRESHOLD) {
            traits.add(highTrait);
        } else if (value <= LOW_THRESHOLD) {
            traits.add(lowTrait);
        } else {
            traits.add(midTrait);
        }
    }

}
