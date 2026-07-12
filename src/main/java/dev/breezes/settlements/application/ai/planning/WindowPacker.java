package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.catalog.CooldownRange;
import dev.breezes.settlements.domain.time.GameTicks;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
     * @param startCivil    civil-space tick (0 = midnight) the slot starts at
     * @param durationTicks estimated duration
     * @param priority      emitted-count-based priority descending from {@code basePriority}
     */
    public record PackedSlot(BehaviorKey key, int startCivil, int durationTicks, int priority) {
    }

    /**
     * Runs the greedy interleaving walk over {@code [startCivil, endCivil)}, delegating the
     * per-step choice among eligible candidates to {@code strategy}.
     * <p>
     * A random start-phase offset in {@code [0, MINIMUM_SLOT_SPACING_TICKS)} is added to the
     * initial cursor so same-profession villagers generated in the same tick don't produce
     * identical slot layouts. The offset draws from the plan's seeded RNG (see {@code random})
     * and is frozen into the persisted plan; the anti-lockstep property still holds because the
     * seed itself is per-villager (UUID-derived) and per-calendar-day, not because the draw is live.
     * This offset and the gap-advance draw belong to the walk itself, not the selection, so they
     * apply identically regardless of which strategy is supplied.
     *
     * @param pool         all candidate behaviors for this window; zero/negative weights are filtered
     * @param startCivil   inclusive start (civil-space tick)
     * @param endCivil     exclusive end
     * @param basePriority priority assigned to the first emitted slot; decrements per emission
     * @param strategy     chooses one candidate among the eligible set at each cursor step
     * @param random       the plan's single seeded RNG instance; shared across every window/strategy
     *                     call in the walk so the whole day's draw order is reproducible from one seed
     * @param packFactor   over-packing factor applied ONLY to the post-emit cursor advance
     * @return mutable list of packed slots; empty when the pool is empty or the window is non-positive
     */
    public static List<PackedSlot> pack(List<PackingCandidate> pool, int startCivil, int endCivil,
                                        int basePriority, SlotSelectionStrategy strategy, Random random,
                                        double packFactor) {
        return pack(pool, startCivil, endCivil, basePriority, strategy, random, packFactor, new HashMap<>());
    }

    /**
     * Overload accepting a caller-supplied {@code nextEligible} baseline, so a caller can thread
     * cadence state across multiple {@code pack} calls (e.g. {@link DayPlanComposer#compose}
     * seeding every band's fill walk with the day's pinned placements before any fill runs — the
     * cross-pass cadence seam, day-plan-llm-wiring-p4.md §2). The map is read as the initial
     * eligibility floor AND mutated in place as this call's own draws are emitted; the caller
     * decides whether to reuse or discard it afterward.
     *
     * @param nextEligible per-key civil tick before which a key is ineligible; a key absent from
     *                     the map defaults to {@code startCivil} (immediately available), exactly
     *                     as the no-baseline overload behaves for every key
     */
    public static List<PackedSlot> pack(List<PackingCandidate> pool, int startCivil, int endCivil,
                                        int basePriority, SlotSelectionStrategy strategy, Random random,
                                        double packFactor, Map<BehaviorKey, Integer> nextEligible) {
        List<PackedSlot> result = new ArrayList<>();
        if (pool.isEmpty() || endCivil <= startCivil) {
            return result;
        }

        // Zero/negative weight = excluded; rest-day multiplier collapses those to 0 before the call
        List<PackingCandidate> candidates = pool.stream()
                .filter(c -> c.weight() > 0)
                .toList();
        if (candidates.isEmpty()) {
            return result;
        }

        Set<BehaviorKey> emittedKeys = new HashSet<>();

        // Seeded phase offset so same-profession villagers (different UUIDs, hence different
        // planSeed) don't produce identical slot positions, while regenerating the SAME villager's
        // plan for the SAME day reproduces this exact offset.
        int cursor = startCivil + random.nextInt(0, MINIMUM_SLOT_SPACING_TICKS - 1 + 1);
        int emitted = 0;

        while (cursor < endCivil) {
            // Capture cursor as effectively-final for lambda
            final int currentCursor = cursor;

            // A candidate is eligible when: its cooldown has elapsed AND it still fits in the window
            List<PackingCandidate> eligible = candidates.stream()
                    .filter(c -> currentCursor >= nextEligible.getOrDefault(c.key(), startCivil))
                    .filter(c -> currentCursor + Math.max(1, c.durationTicks()) <= endCivil)
                    .toList();

            if (eligible.isEmpty()) {
                // No behavior fits right now — leave a gap
                cursor += random.nextInt(MINIMUM_SLOT_SPACING_TICKS / 2, MINIMUM_SLOT_SPACING_TICKS + 1);
                continue;
            }

            PackingCandidate chosen = strategy.select(eligible, emittedKeys, random);
            int duration = Math.max(1, chosen.durationTicks());

            result.add(new PackedSlot(chosen.key(), cursor, duration, Math.max(0, basePriority - emitted)));
            emittedKeys.add(chosen.key());

            // Space this key's next appearance by a random draw from its cooldown range
            // Cooldowns are authored in real time (ClockTicks); we apply them directly as plan-timeline offsets
            int cooldownSpacing = Math.max(MINIMUM_SLOT_SPACING_TICKS, chosen.cooldown().drawTicks(random));
            nextEligible.put(chosen.key(), cursor + cooldownSpacing);

            emitted++;
            // Over-packing: behaviors usually finish under their estimate, so the cursor only
            // advances by a packFactor-scaled fraction of the duration, not the full duration
            int packedAdvance = (int) Math.round(duration * packFactor);
            cursor += Math.max(MINIMUM_SLOT_SPACING_TICKS, packedAdvance);
        }

        return result;
    }

}
