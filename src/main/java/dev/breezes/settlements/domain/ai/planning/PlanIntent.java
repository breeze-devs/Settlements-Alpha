package dev.breezes.settlements.domain.ai.planning;

import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * What the LLM overlay contributes to plan composition beyond the raw {@link PlanGenerationContext}:
 * an ordered, per-band behavior selection (band id → ordered list of behavior keys, best-first),
 * plus fixed-time {@link PinnedSelection}s the composer's anchor stage tries to place before fill runs.
 */
public record PlanIntent(Map<String, List<BehaviorKey>> selections, List<PinnedSelection> pins) {

    private static final PlanIntent EMPTY = new PlanIntent(Map.of(), List.of());

    public PlanIntent {
        // Deep-copy
        selections = selections.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
        pins = List.copyOf(pins);
    }

    /**
     * The pure-heuristic sentinel: a composer call carrying this intent fills every band from its
     * own derived pool, exactly as the heuristic generator always has.
     * <p>
     * This is deliberately distinct from "the LLM considered every band and selected nothing" —
     * that case still carries one (empty-list) entry per band the overlay considered, so its
     * {@link #selections()} map is never itself empty. Only the true absence of an LLM result
     * collapses to this sentinel, which is why callers must build intents by populating one entry
     * per band rather than by pruning empty ones away. {@link #pins()} is empty here too — both
     * conditions together are what {@link #isEmpty()} checks.
     */
    public static PlanIntent empty() {
        return EMPTY;
    }

    public boolean isEmpty() {
        return this.selections.isEmpty() && this.pins.isEmpty();
    }

    /**
     * Builds an intent carrying only fixed-time pins forward across a plan regeneration (e.g. a
     * hard reset), with no per-band selections.
     * <p>
     * A carry-forward caller never has an LLM-considered band set to report, so populating
     * {@link #selections()} here would violate the "one entry per considered band" contract the
     * LLM-overlay path relies on. Leaving it empty is also what routes the composer back through
     * its ordinary heuristic gap-fill (see {@code DayPlanComposer#fillPolicyFor}) — the carried
     * pins re-enter composition only through the anchor stage.
     */
    public static PlanIntent ofCarriedPins(List<PinnedSelection> pins) {
        return new PlanIntent(Map.of(), pins);
    }

}
