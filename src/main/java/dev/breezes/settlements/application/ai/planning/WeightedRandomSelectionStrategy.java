package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.application.ai.planning.WindowPacker.PackingCandidate;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.shared.util.RandomUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Weighted-random selection with a fresh-first coverage bias.
 * <p>
 * Prefers keys that have not yet been emitted in this window (coverage-first pass); if all
 * eligible keys have already appeared, all eligible candidates compete. This is the
 * historical behavior of {@link WindowPacker} and its determinism-on-reload guarantee
 * depends on this exact sequencing — do not alter the accumulation order.
 */
public final class WeightedRandomSelectionStrategy implements SlotSelectionStrategy {

    @Override
    public PackingCandidate select(List<PackingCandidate> eligible, Set<BehaviorKey> emittedKeys, Random random) {
        List<PackingCandidate> fresh = eligible.stream()
                .filter(c -> !emittedKeys.contains(c.key()))
                .toList();

        List<PackingCandidate> pool = fresh.isEmpty() ? eligible : fresh;

        // Cumulative-weight selection using a LinkedHashMap to preserve insertion order for the
        // edge case where floating-point accumulation undershoots totalWeight (last entry wins).
        Map<PackingCandidate, Double> weightMap = new LinkedHashMap<>();
        for (PackingCandidate candidate : pool) {
            weightMap.put(candidate, candidate.weight());
        }

        return RandomUtil.weightedChoice(weightMap, random);
    }

}
