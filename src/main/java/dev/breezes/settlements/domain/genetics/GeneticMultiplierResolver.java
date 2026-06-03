package dev.breezes.settlements.domain.genetics;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class GeneticMultiplierResolver {

    private static final double CENTERED_GENE_VALUE = 0.5;

    public static double centeredMultiplier(double gene, double impact) {
        return 1.0 + ((gene - CENTERED_GENE_VALUE) * 2.0 * impact);
    }

}
