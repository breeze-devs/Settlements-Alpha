package dev.breezes.settlements.domain.genetics;

import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

/**
 * Raw structured signal for a single gene dimension: which gene, and its un-banded 0–1 value.
 */
@Builder
public record GeneSignal(GeneType dimension, double value) {

    /**
     * Derives one signal per gene dimension, in {@link GeneType#VALUES} order.
     */
    public static List<GeneSignal> allFrom(GeneticsProfile genetics) {
        List<GeneSignal> signals = new ArrayList<>(GeneType.VALUES.length);
        for (GeneType type : GeneType.VALUES) {
            signals.add(GeneSignal.builder()
                    .dimension(type)
                    .value(genetics.getGeneValue(type))
                    .build());
        }
        return signals;
    }

}
