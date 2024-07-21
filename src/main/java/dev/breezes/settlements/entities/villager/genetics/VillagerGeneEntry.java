package dev.breezes.settlements.entities.villager.genetics;

import dev.breezes.settlements.util.RandomUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * Represents one genetic trait entry for a villager
 */
@AllArgsConstructor
@Getter
public class VillagerGeneEntry {

    private final VillagerGeneType type;
    private final double value;

    /**
     * Create a new gene entry with a random value
     */
    public VillagerGeneEntry(VillagerGeneType type) {
        this(type, type.randomValue());
    }

    /**
     * Copy constructor
     */
    public VillagerGeneEntry(VillagerGeneEntry entry) {
        this(entry.getType(), entry.getValue());
    }


    /**
     * Performs a genetic crossover between this and another VillagerGeneEntry, considering mutation
     *
     * @param other        the other VillagerGeneEntry to cross over with; must be of the same type
     * @param weight       the weight of this gene's value in the crossover, where 1 fully favors this gene, and 0 fully favors the other gene
     * @param mutationRate the probability of a mutation occurring, causing the resulting gene to have a random value
     * @return new VillagerGeneEntry of the same type with the crossover value
     * @throws IllegalArgumentException if gene types do not match
     */
    public VillagerGeneEntry crossOver(VillagerGeneEntry other, double weight, double mutationRate)
            throws IllegalArgumentException {
        if (this.getType() != other.getType()) {
            throw new IllegalArgumentException("Cannot cross over different gene types");
        }

        double newValue = RandomUtil.RANDOM.nextDouble() < mutationRate
                ? this.getType().randomValue()
                : this.getValue() * weight + other.getValue() * (1 - weight);

        return new VillagerGeneEntry(this.getType(), newValue);
    }

}
