package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.catalog.CooldownRange;
import dev.breezes.settlements.domain.time.GameTicks;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Greedy timeline packer.
 * <p>
 * Density comes from diversity: the packer interleaves many different keys, each spaced
 * by its own drawn cooldown, and leaves gaps when no key is currently eligible. This
 * fixes the old Hamilton-allocation model that produced density by repeating a single key.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class WindowPacker {

    /**
     * Minimum gap the cursor advances when no candidate is eligible, and the minimum
     * spacing enforced between consecutive placements of any key regardless of cooldown.
     * Moved here from {@link HeuristicPlanGenerator} because spacing is a packing concern.
     */
    public static final int MINIMUM_SLOT_SPACING_TICKS = GameTicks.minutes(10).getTicksAsInt();

    /**
     * Carries all information the packer needs about a single behavior variant.
     *
     * @param key           stable behavior identifier
     * @param weight        relative selection weight; must be > 0 to be considered
     * @param durationTicks estimated duration of one execution; clamped to >= 1 internally
     * @param cooldown      drawn once per emission to set the key's next-eligible time
     */
    public record PackingCandidate(BehaviorKey key, double weight, int durationTicks, CooldownRange cooldown) {
    }

    /**
     * A single slot produced by the packing algorithm.
     *
     * @param key           behavior to execute
     * @param startLinear   linear tick offset from the window's start
     * @param durationTicks estimated duration
     * @param priority      emitted-count-based priority descending from {@code basePriority}
     */
    public record PackedSlot(BehaviorKey key, int startLinear, int durationTicks, int priority) {
    }

    /**
     * Runs the greedy interleaving walk over {@code [startLinear, endLinear)}.
     * <p>
     * Selection preference: within each cursor step, the algorithm first restricts eligible
     * candidates to those whose key has not yet been emitted in this window (the "fresh-first"
     * bias. If that subset is empty, it falls back to all eligible candidates. The final pick
     * is weighted-random by {@code weight}.
     * <p>
     * A random start-phase offset in {@code [0, MINIMUM_SLOT_SPACING_TICKS)} is added to the
     * initial cursor so same-profession villagers generated in the same tick don't produce
     * identical slot layouts. The phase uses live RNG and is frozen into the persisted plan.
     *
     * @param pool         all candidate behaviors for this window; zero/negative weights are filtered
     * @param startLinear  inclusive start (in linear ticks from the plan epoch)
     * @param endLinear    exclusive end
     * @param basePriority priority assigned to the first emitted slot; decrements per emission
     * @return mutable list of packed slots; empty when the pool is empty or the window is non-positive
     */
    public static List<PackedSlot> pack(List<PackingCandidate> pool, int startLinear, int endLinear, int basePriority) {
        List<PackedSlot> result = new ArrayList<>();
        if (pool.isEmpty() || endLinear <= startLinear) {
            return result;
        }

        // Zero/negative weight = excluded; rest-day multiplier collapses those to 0 before the call
        List<PackingCandidate> candidates = pool.stream()
                .filter(c -> c.weight() > 0)
                .toList();
        if (candidates.isEmpty()) {
            return result;
        }

        // Per-key next-eligible linear tick; defaults to startLinear (immediately available)
        Map<BehaviorKey, Integer> nextEligible = new HashMap<>();
        Set<BehaviorKey> emittedKeys = new HashSet<>();

        // Small live-RNG phase offset so same-profession villagers generated concurrently
        // don't produce identical slot positions, without breaking plan determinism on reload.
        int cursor = startLinear + RandomUtil.randomInt(0, MINIMUM_SLOT_SPACING_TICKS - 1, true);
        int emitted = 0;

        while (cursor < endLinear) {
            // Capture cursor as effectively-final for lambda
            final int currentCursor = cursor;

            // A candidate is eligible when: its cooldown has elapsed AND it still fits in the window
            List<PackingCandidate> eligible = candidates.stream()
                    .filter(c -> currentCursor >= nextEligible.getOrDefault(c.key(), startLinear))
                    .filter(c -> currentCursor + Math.max(1, c.durationTicks()) <= endLinear)
                    .toList();

            if (eligible.isEmpty()) {
                // No behavior fits right now — leave a gap
                cursor += RandomUtil.randomInt(MINIMUM_SLOT_SPACING_TICKS / 2, MINIMUM_SLOT_SPACING_TICKS, true);
                continue;
            }

            PackingCandidate chosen = pick(eligible, emittedKeys);
            int duration = Math.max(1, chosen.durationTicks());

            result.add(new PackedSlot(chosen.key(), cursor, duration, Math.max(0, basePriority - emitted)));
            emittedKeys.add(chosen.key());

            // Space this key's next appearance by a random draw from its cooldown range
            // Cooldowns are authored in real time (ClockTicks); we apply them directly as plan-timeline offsets
            int cooldownSpacing = Math.max(MINIMUM_SLOT_SPACING_TICKS, chosen.cooldown().drawTicks());
            nextEligible.put(chosen.key(), cursor + cooldownSpacing);

            emitted++;
            cursor += Math.max(duration, MINIMUM_SLOT_SPACING_TICKS);
        }

        return result;
    }

    /**
     * Picks one candidate using weighted-random selection.
     * <p>
     * Prefers keys that have not yet been emitted in this window (coverage-first pass); if all
     * eligible keys have already appeared, all eligible candidates compete.
     */
    private static PackingCandidate pick(List<PackingCandidate> eligible, Set<BehaviorKey> emittedKeys) {
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

        return RandomUtil.weightedChoice(weightMap);
    }

}
