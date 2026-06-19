package dev.breezes.settlements.application.ai.inference.monologue;

import dev.breezes.settlements.domain.ai.knowledge.KnowledgeEntry;
import dev.breezes.settlements.domain.ai.knowledge.VillagerKnowledgeStore;

import java.util.Comparator;
import java.util.List;

/**
 * Selects the most salient knowledge entries to use as grounding seeds in a MONOLOGUE request.
 * <p>
 * Seeds are the primary divergence source between villagers: they ground the model in
 * per-villager episodic memory rather than generic profession flavour. Lean towards the
 * service for humanization; we ship the raw content strings and let the backend phrase them.
 */
public final class MonologueSeedSelector {

    /**
     * Hard cap on seeds sent per villager. Kept as a named constant because it is a
     * deliberate quality/token-budget trade-off, not a tuning knob exposed to operators.
     */
    public static final int MAX_SEEDS = 5;

    private MonologueSeedSelector() {
    }

    /**
     * Returns up to {@link #MAX_SEEDS} content strings from the store, ranked by weight
     * descending with admission-tick recency as a tie-breaker.
     * <p>
     * Weight captures both original importance and corroboration bumps, making it a better
     * proxy for "what is this villager most preoccupied by" than raw recency or insertion order.
     */
    public static List<String> select(VillagerKnowledgeStore store) {
        // Build descending comparators explicitly — chaining .reversed() on a thenComparing
        // result would only invert the secondary key, not the primary.
        Comparator<KnowledgeEntry> byWeightDesc = Comparator
                .comparingDouble(KnowledgeEntry::getWeight)
                .reversed();
        Comparator<KnowledgeEntry> byRecencyDesc = Comparator
                .comparingLong(KnowledgeEntry::getAdmittedAtTick)
                .reversed();

        return store.entriesView().stream()
                .sorted(byWeightDesc.thenComparing(byRecencyDesc))
                .limit(MAX_SEEDS)
                .map(KnowledgeEntry::getContent)
                .toList();
    }

}
