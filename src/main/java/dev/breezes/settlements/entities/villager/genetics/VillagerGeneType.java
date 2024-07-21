package dev.breezes.settlements.entities.villager.genetics;

import dev.breezes.settlements.util.RandomUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enum representing different types of villager genes that influences certain characteristics
 */
@AllArgsConstructor
public enum VillagerGeneType {

    /**
     * Multiplier for max health
     * Transformation: Max HP = default max HP * (gene + 0.5)
     */
    HEALTH(0, 1, (gene, original) -> (gene + 0.5) * original),
    /**
     * Influences attack damage and knockback
     */
    STRENGTH(0, 1, (gene, original) -> original),
    /**
     * Influences movement speed
     */
    FITNESS(0, 1, (gene, original) -> original),
    /**
     * Influences ??
     */
    COURAGE(0, 1, (gene, original) -> original);


    @Getter
    private final double lowerBound;
    @Getter
    private final double upperBound;

    private final VillagerGeneTransformer transformer;


    /**
     * Generates a random value for the gene within its specified bounds.
     *
     * @return A random double value within the bounds of the gene.
     */
    public double randomValue() {
        return RandomUtil.randomDouble(this.lowerBound, this.upperBound);
    }

    /**
     * Transforms the gene value based on the original characteristic value
     *
     * @param gene     The gene value to be transformed
     * @param original The original characteristic value
     * @return The transformed value
     */
    public double transform(double gene, double original) {
        return this.transformer.transform(gene, original);
    }


    /**
     * Functional interface for transforming gene values
     */
    private interface VillagerGeneTransformer {
        double transform(double gene, double original);
    }

}
