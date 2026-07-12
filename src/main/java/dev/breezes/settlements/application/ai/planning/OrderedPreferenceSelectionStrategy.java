package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.application.ai.planning.WindowPacker.PackingCandidate;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Deterministic {@link SlotSelectionStrategy} driven by a caller-supplied preference order,
 * for the SIS day-planning path: the model picks and orders behaviors from the closed
 * menu, and this strategy honors that order exactly rather than drawing randomly.
 * <p>
 * Semantics mirror {@link WeightedRandomSelectionStrategy}'s coverage-first bias, but the
 * tie-break within (fresh vs. repeat) is "earliest in the supplied order" instead of a
 * weighted draw, so the same order always packs the same window identically.
 * <p>
 * Keys eligible this step but absent from the supplied order rank after every supplied key —
 * they are the unranked tail — and are only chosen when no supplied key is eligible. Ties
 * within the tail (multiple unranked keys, or none) are broken by {@link BehaviorKey#id()} so
 * the outcome never depends on pool iteration order.
 */
public final class OrderedPreferenceSelectionStrategy implements SlotSelectionStrategy {

    private final Map<BehaviorKey, Integer> rank;

    public OrderedPreferenceSelectionStrategy(List<BehaviorKey> preferenceOrder) {
        // Duplicate keys keep their first index; later duplicates must not overwrite it.
        Map<BehaviorKey, Integer> ranks = new HashMap<>();
        for (int i = 0; i < preferenceOrder.size(); i++) {
            ranks.putIfAbsent(preferenceOrder.get(i), i);
        }
        this.rank = ranks;
    }

    @Override
    public PackingCandidate select(List<PackingCandidate> eligible, Set<BehaviorKey> emittedKeys, Random random) {
        // Random is intentionally unused: this strategy is deliberately deterministic
        List<PackingCandidate> fresh = eligible.stream()
                .filter(c -> !emittedKeys.contains(c.key()))
                .toList();

        List<PackingCandidate> pool = fresh.isEmpty() ? eligible : fresh;

        return pool.stream()
                .min(Comparator.comparing(this::rankOf).thenComparing(c -> c.key().id()))
                .orElseThrow();
    }

    /**
     * Supplied-order rank, or {@link Integer#MAX_VALUE} for keys absent from the order — pushing
     * the entire unranked tail after every explicitly ranked key regardless of pool size.
     */
    private int rankOf(PackingCandidate candidate) {
        return this.rank.getOrDefault(candidate.key(), Integer.MAX_VALUE);
    }

}
