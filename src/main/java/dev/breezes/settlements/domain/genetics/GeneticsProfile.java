package dev.breezes.settlements.domain.genetics;

import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the complete genetic makeup of a single entity.
 */
@CustomLog
public class GeneticsProfile {

    private final Map<GeneType, Gene> genes;

    // TODO: move to configurations
    private static final double DEFAULT_MUTATION_RATE = 0.05;
    private static final double DEFAULT_WILD_MUTATION_CHANCE = 0.01;

    /**
     * Initialize this profile with random genes
     */
    public GeneticsProfile() {
        this.genes = new EnumMap<>(GeneType.class);
        for (GeneType type : GeneType.VALUES) {
            double baseValue = Mth.clamp(RandomUtil.randomGaussian(0.4, 0.133), 0.0, 1.0);
            this.setGene(type, new Gene(baseValue));
        }
    }

    public GeneticsProfile(Map<GeneType, Gene> genes) {
        this.genes = new EnumMap<>(genes);
    }

    public Optional<Gene> getGene(GeneType type) {
        return Optional.ofNullable(this.genes.get(type));
    }

    public double getGeneValue(GeneType type) {
        return this.getGene(type)
                .map(Gene::value)
                .orElse(0.5);
    }

    public void setGene(GeneType type, Gene gene) {
        this.genes.put(type, gene);
    }

    public Map<GeneType, Gene> getAllGenes() {
        return Collections.unmodifiableMap(this.genes);
    }

    public GeneticsProfile copy() {
        return new GeneticsProfile(this.genes);
    }

    /**
     * Create a new child profile by crossing over this profile with the other parent.
     */
    public GeneticsProfile crossover(GeneticsProfile other, RandomSource random) {
        GeneticsProfile child = new GeneticsProfile();
        for (GeneType type : GeneType.VALUES) {
            double valA = this.getGeneValue(type);
            double valB = other.getGeneValue(type);

            double childValue;
            if (random.nextDouble() < DEFAULT_WILD_MUTATION_CHANCE) {
                childValue = random.nextDouble();
            } else {
                double average = (valA + valB) / 2.0;
                double mutation = (random.nextDouble() * (DEFAULT_MUTATION_RATE * 2)) - DEFAULT_MUTATION_RATE;
                childValue = Mth.clamp(average + mutation, 0.0, 1.0);
            }
            child.setGene(type, new Gene(childValue));
        }
        return child;

    }

}
