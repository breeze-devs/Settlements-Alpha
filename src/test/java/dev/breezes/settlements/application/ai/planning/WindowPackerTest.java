package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.catalog.CooldownRange;
import dev.breezes.settlements.domain.time.ClockTicks;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure (no Minecraft) invariant tests for {@link WindowPacker}.
 * <p>
 * All assertions must hold for any RNG outcome, so they are expressed as invariants
 * rather than exact positions. Weighted-random-strategy tests construct a fresh, unseeded
 * {@link Random} per invocation (mirroring production's per-{@code generate()} instance) so
 * repetitions still explore different draw sequences; ordered-strategy tests pass a fixed seed
 * since that strategy ignores it entirely and the outcome is exact-value deterministic anyway.
 */
class WindowPackerTest {

    private static final int MIN_SPACING = WindowPacker.MINIMUM_SLOT_SPACING_TICKS;

    // Every test below packs through this explicit strategy — WindowPacker no longer has a default
    // (the production heuristic path now supplies its own strategy instance via DayPlanComposer).
    private static final SlotSelectionStrategy WEIGHTED_RANDOM = new WeightedRandomSelectionStrategy();

    // Reusable cooldown fixtures — expressed in real ticks (ClockTicks), NOT game ticks
    private static final CooldownRange MINIMUM_COOLDOWN = CooldownRange.ofSeconds(1, 1);
    private static final CooldownRange SHORT_COOLDOWN = CooldownRange.builder()
            .min(ClockTicks.of(MIN_SPACING))
            .max(ClockTicks.of(MIN_SPACING))
            .build();
    private static final CooldownRange LARGE_COOLDOWN = CooldownRange.builder()
            .min(ClockTicks.of(MIN_SPACING * 4L))
            .max(ClockTicks.of(MIN_SPACING * 4L))
            .build();

    private static final BehaviorKey KEY_A = BehaviorKey.of("test_a");
    private static final BehaviorKey KEY_B = BehaviorKey.of("test_b");
    private static final BehaviorKey KEY_C = BehaviorKey.of("test_c");

    @Test
    void pack_emptyPool_returnsEmpty() {
        // Arrange
        List<WindowPacker.PackingCandidate> pool = List.of();

        // Act
        List<WindowPacker.PackedSlot> result = WindowPacker.pack(pool, 0, MIN_SPACING * 10, 50, WEIGHTED_RANDOM, new Random(0L), 1.0);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void pack_nonPositiveWindow_returnsEmpty() {
        // Arrange
        List<WindowPacker.PackingCandidate> pool = List.of(candidate(KEY_A, 1.0, MIN_SPACING, MINIMUM_COOLDOWN));

        // Act — endCivil == startCivil
        List<WindowPacker.PackedSlot> sameEdge = WindowPacker.pack(pool, 100, 100, 50, WEIGHTED_RANDOM, new Random(0L), 1.0);
        // Act — endCivil < startCivil
        List<WindowPacker.PackedSlot> inverted = WindowPacker.pack(pool, 200, 100, 50, WEIGHTED_RANDOM, new Random(0L), 1.0);

        // Assert
        assertTrue(sameEdge.isEmpty());
        assertTrue(inverted.isEmpty());
    }

    @Test
    void pack_zeroWeightCandidates_returnsEmpty() {
        // Arrange — weight == 0 means "excluded" (rest-day suppression mechanism)
        List<WindowPacker.PackingCandidate> pool = List.of(
                candidate(KEY_A, 0.0, MIN_SPACING, SHORT_COOLDOWN),
                candidate(KEY_B, 0.0, MIN_SPACING, SHORT_COOLDOWN)
        );

        // Act
        List<WindowPacker.PackedSlot> result = WindowPacker.pack(pool, 0, MIN_SPACING * 10, 50, WEIGHTED_RANDOM, new Random(0L), 1.0);

        // Assert
        assertTrue(result.isEmpty());
    }

    @RepeatedTest(30)
    void pack_allSlotsFitWithinWindow() {
        // Arrange
        int start = 0;
        int end = MIN_SPACING * 20;
        List<WindowPacker.PackingCandidate> pool = List.of(
                candidate(KEY_A, 1.0, MIN_SPACING, SHORT_COOLDOWN),
                candidate(KEY_B, 1.0, MIN_SPACING, SHORT_COOLDOWN),
                candidate(KEY_C, 1.0, MIN_SPACING, SHORT_COOLDOWN)
        );

        // Act
        List<WindowPacker.PackedSlot> result = WindowPacker.pack(pool, start, end, 50, WEIGHTED_RANDOM, new Random(), 1.0);

        // Assert — every slot's [startCivil, startCivil+duration) must fit within [start, end)
        for (WindowPacker.PackedSlot slot : result) {
            assertTrue(slot.startCivil() >= start,
                    "startCivil " + slot.startCivil() + " must be >= " + start);
            assertTrue(slot.startCivil() + slot.durationTicks() <= end,
                    "slot end " + (slot.startCivil() + slot.durationTicks()) + " must be <= " + end);
        }
    }

    @RepeatedTest(30)
    void pack_singleKeyLargeCooldown_consecutiveSlotsAreSpacedByCooldown() {
        // Arrange — one key with a 4× spacing cooldown
        int start = 0;
        int end = MIN_SPACING * 30;
        int expectedSpacing = MIN_SPACING * 4;
        List<WindowPacker.PackingCandidate> pool = List.of(
                candidate(KEY_A, 1.0, MIN_SPACING, LARGE_COOLDOWN)
        );

        // Act
        List<WindowPacker.PackedSlot> result = WindowPacker.pack(pool, start, end, 50, WEIGHTED_RANDOM, new Random(), 1.0);

        // Assert — consecutive appearances of the same key must be at least cooldown apart
        List<WindowPacker.PackedSlot> keyASlots = result.stream()
                .filter(s -> s.key().equals(KEY_A))
                .toList();

        for (int i = 1; i < keyASlots.size(); i++) {
            int gap = keyASlots.get(i).startCivil() - keyASlots.get(i - 1).startCivil();
            assertTrue(gap >= expectedSpacing,
                    "Consecutive KEY_A slots must be >= " + expectedSpacing + " apart, got " + gap);
        }
    }

    @RepeatedTest(30)
    void pack_singleKeyLargeCooldown_windowHasGaps() {
        // Arrange — single key with large cooldown should leave most of the window empty
        int start = 0;
        int end = MIN_SPACING * 30;
        List<WindowPacker.PackingCandidate> pool = List.of(
                candidate(KEY_A, 1.0, MIN_SPACING, LARGE_COOLDOWN)
        );

        // Act
        List<WindowPacker.PackedSlot> result = WindowPacker.pack(pool, start, end, 50, WEIGHTED_RANDOM, new Random(), 1.0);

        // Assert — total occupied ticks < window length (gaps exist)
        int totalOccupied = result.stream().mapToInt(WindowPacker.PackedSlot::durationTicks).sum();
        assertTrue(totalOccupied < (end - start),
                "Single-key pool with large cooldown should leave gaps; occupied=" + totalOccupied);
    }

    @RepeatedTest(30)
    void pack_singleKeyLargeCooldown_slotCountIsBoundedByCooldown() {
        // Arrange
        int start = 0;
        int end = MIN_SPACING * 30;
        int cooldownSpacing = MIN_SPACING * 4;
        List<WindowPacker.PackingCandidate> pool = List.of(
                candidate(KEY_A, 1.0, MIN_SPACING, LARGE_COOLDOWN)
        );

        // Act
        List<WindowPacker.PackedSlot> result = WindowPacker.pack(pool, start, end, 50, WEIGHTED_RANDOM, new Random(), 1.0);

        // Assert — can't appear more often than the window / cooldown allows
        int maxExpected = (end - start) / cooldownSpacing + 1;
        assertTrue(result.size() <= maxExpected,
                "Slot count " + result.size() + " exceeds max expected " + maxExpected);
    }

    @RepeatedTest(30)
    void pack_diversePool_everyKeyAppearsAtLeastOnce() {
        // Arrange — window wide enough for all three keys to fit at least once
        int start = 0;
        int end = MIN_SPACING * 20;
        List<WindowPacker.PackingCandidate> pool = List.of(
                candidate(KEY_A, 1.0, MIN_SPACING, SHORT_COOLDOWN),
                candidate(KEY_B, 1.0, MIN_SPACING, SHORT_COOLDOWN),
                candidate(KEY_C, 1.0, MIN_SPACING, SHORT_COOLDOWN)
        );

        // Act
        List<WindowPacker.PackedSlot> result = WindowPacker.pack(pool, start, end, 50, WEIGHTED_RANDOM, new Random(), 1.0);

        // Assert
        assertFalse(result.isEmpty());
        Map<BehaviorKey, Long> countsByKey = result.stream()
                .collect(Collectors.groupingBy(WindowPacker.PackedSlot::key, Collectors.counting()));

        assertTrue(countsByKey.containsKey(KEY_A), "KEY_A must appear at least once");
        assertTrue(countsByKey.containsKey(KEY_B), "KEY_B must appear at least once");
        assertTrue(countsByKey.containsKey(KEY_C), "KEY_C must appear at least once");
    }

    @RepeatedTest(30)
    void pack_diversePool_noTwoSameKeySlotsCloserThanCooldown() {
        // Arrange
        int start = 0;
        int end = MIN_SPACING * 30;
        List<WindowPacker.PackingCandidate> pool = List.of(
                candidate(KEY_A, 1.0, MIN_SPACING, SHORT_COOLDOWN),
                candidate(KEY_B, 1.0, MIN_SPACING, SHORT_COOLDOWN),
                candidate(KEY_C, 1.0, MIN_SPACING, SHORT_COOLDOWN)
        );

        // Act
        List<WindowPacker.PackedSlot> result = WindowPacker.pack(pool, start, end, 50, WEIGHTED_RANDOM, new Random(), 1.0);

        // Assert — for each key, check consecutive appearances are >= cooldown apart
        Map<BehaviorKey, List<WindowPacker.PackedSlot>> byKey = result.stream()
                .collect(Collectors.groupingBy(WindowPacker.PackedSlot::key));

        for (Map.Entry<BehaviorKey, List<WindowPacker.PackedSlot>> entry : byKey.entrySet()) {
            List<WindowPacker.PackedSlot> keySlots = entry.getValue();
            for (int i = 1; i < keySlots.size(); i++) {
                int gap = keySlots.get(i).startCivil() - keySlots.get(i - 1).startCivil();
                assertTrue(gap >= MIN_SPACING,
                        "Key " + entry.getKey().id() + " slots " + (i - 1) + " and " + i
                                + " are only " + gap + " ticks apart (min=" + MIN_SPACING + ")");
            }
        }
    }

    @RepeatedTest(20)
    void pack_priorityDecreasesWithEachEmittedSlot() {
        // Arrange
        int basePriority = 70;
        int start = 0;
        int end = MIN_SPACING * 20;
        List<WindowPacker.PackingCandidate> pool = List.of(
                candidate(KEY_A, 1.0, MIN_SPACING, SHORT_COOLDOWN),
                candidate(KEY_B, 1.0, MIN_SPACING, SHORT_COOLDOWN)
        );

        // Act
        List<WindowPacker.PackedSlot> result = WindowPacker.pack(pool, start, end, basePriority, WEIGHTED_RANDOM, new Random(), 1.0);

        // Assert — slots appear in emission order; first must have the highest priority
        assertFalse(result.isEmpty());
        assertEquals(basePriority, result.get(0).priority());

        for (int i = 1; i < result.size(); i++) {
            assertTrue(result.get(i).priority() <= result.get(i - 1).priority(),
                    "Priority must be non-increasing across emitted slots");
            assertTrue(result.get(i).priority() >= 0, "Priority must be non-negative");
        }
    }

    @RepeatedTest(20)
    void pack_mixedWeights_zeroWeightCandidatesNeverAppear() {
        // Arrange — KEY_B has zero weight and must be excluded even when KEY_A's cooldown is active
        int start = 0;
        int end = MIN_SPACING * 20;
        List<WindowPacker.PackingCandidate> pool = List.of(
                candidate(KEY_A, 1.0, MIN_SPACING, SHORT_COOLDOWN),
                candidate(KEY_B, 0.0, MIN_SPACING, SHORT_COOLDOWN)
        );

        // Act
        List<WindowPacker.PackedSlot> result = WindowPacker.pack(pool, start, end, 50, WEIGHTED_RANDOM, new Random(), 1.0);

        // Assert
        assertFalse(result.isEmpty());
        assertTrue(result.stream().noneMatch(s -> s.key().equals(KEY_B)),
                "Zero-weight key must never appear in the output");
    }

    @Test
    void pack_orderedStrategy_coverageFirstThenHonorsSuppliedOrder() {
        // Arrange — wide window, cheap cooldowns, order deliberately not matching pool declaration order
        int start = 0;
        int end = MIN_SPACING * 20;
        List<WindowPacker.PackingCandidate> pool = List.of(
                candidate(KEY_A, 1.0, MIN_SPACING, SHORT_COOLDOWN),
                candidate(KEY_B, 1.0, MIN_SPACING, SHORT_COOLDOWN),
                candidate(KEY_C, 1.0, MIN_SPACING, SHORT_COOLDOWN)
        );
        OrderedPreferenceSelectionStrategy strategy = new OrderedPreferenceSelectionStrategy(List.of(KEY_B, KEY_A, KEY_C));

        // Act
        List<WindowPacker.PackedSlot> result = WindowPacker.pack(pool, start, end, 50, strategy, new Random(0L), 1.0);

        // Assert — coverage-first guarantees each fresh key is emitted before any repeat, and within
        // the fresh pass the earliest-in-order key always wins, so the first three slots are exactly B, A, C
        assertTrue(result.size() >= 3, "Expected all three fresh keys to be emitted");
        assertEquals(KEY_B, result.get(0).key());
        assertEquals(KEY_A, result.get(1).key());
        assertEquals(KEY_C, result.get(2).key());

        // Once every key has appeared once, the walk is in the repeat phase — order still governs
        for (int i = 3; i < result.size(); i++) {
            assertEquals(KEY_B, result.get(i).key(), "After coverage, repeats must follow supplied order starting from B");
        }
    }

    @Test
    void pack_orderedStrategy_foreignKeyInOrderIsIgnored() {
        // Arrange — the supplied order references a key that isn't in the pool at all
        int start = 0;
        int end = MIN_SPACING * 20;
        List<WindowPacker.PackingCandidate> pool = List.of(
                candidate(KEY_A, 1.0, MIN_SPACING, SHORT_COOLDOWN),
                candidate(KEY_B, 1.0, MIN_SPACING, SHORT_COOLDOWN)
        );
        BehaviorKey foreignKey = BehaviorKey.of("test_foreign");
        OrderedPreferenceSelectionStrategy strategy = new OrderedPreferenceSelectionStrategy(List.of(foreignKey, KEY_B, KEY_A));

        // Act
        List<WindowPacker.PackedSlot> result = WindowPacker.pack(pool, start, end, 50, strategy, new Random(0L), 1.0);

        // Assert — the foreign key never appears; real pool keys are still ordered B before A
        assertTrue(result.size() >= 2);
        assertTrue(result.stream().noneMatch(s -> s.key().equals(foreignKey)));
        assertEquals(KEY_B, result.get(0).key());
        assertEquals(KEY_A, result.get(1).key());
    }

    @Test
    void pack_orderedStrategy_keyAbsentFromOrderFallsToTail() {
        // Arrange — KEY_C is eligible but not present in the supplied order at all
        int start = 0;
        int end = MIN_SPACING * 20;
        List<WindowPacker.PackingCandidate> pool = List.of(
                candidate(KEY_A, 1.0, MIN_SPACING, SHORT_COOLDOWN),
                candidate(KEY_B, 1.0, MIN_SPACING, SHORT_COOLDOWN),
                candidate(KEY_C, 1.0, MIN_SPACING, SHORT_COOLDOWN)
        );
        OrderedPreferenceSelectionStrategy strategy = new OrderedPreferenceSelectionStrategy(List.of(KEY_B, KEY_A));

        // Act
        List<WindowPacker.PackedSlot> result = WindowPacker.pack(pool, start, end, 50, strategy, new Random(0L), 1.0);

        // Assert — unranked KEY_C is only picked once both ranked keys have been covered
        assertTrue(result.size() >= 3);
        assertEquals(KEY_B, result.get(0).key());
        assertEquals(KEY_A, result.get(1).key());
        assertEquals(KEY_C, result.get(2).key());
    }

    @Test
    void pack_orderedStrategy_emptyOrderTreatsAllKeysAsUnrankedTail() {
        // Arrange — no supplied order at all; every eligible key is unranked, so tie-break is by id
        int start = 0;
        int end = MIN_SPACING * 20;
        List<WindowPacker.PackingCandidate> pool = List.of(
                candidate(KEY_B, 1.0, MIN_SPACING, SHORT_COOLDOWN),
                candidate(KEY_A, 1.0, MIN_SPACING, SHORT_COOLDOWN)
        );
        OrderedPreferenceSelectionStrategy strategy = new OrderedPreferenceSelectionStrategy(List.of());

        // Act
        List<WindowPacker.PackedSlot> result = WindowPacker.pack(pool, start, end, 50, strategy, new Random(0L), 1.0);

        // Assert — deterministic id tie-break means KEY_A (id "test_a") always wins over KEY_B first
        assertTrue(result.size() >= 2);
        assertEquals(KEY_A, result.get(0).key());
        assertEquals(KEY_B, result.get(1).key());
    }

    @Test
    void pack_orderedStrategy_duplicateKeysInOrderKeepFirstIndex() {
        // Arrange — KEY_A appears twice in the supplied order; the later, lower-priority index must not win
        int start = 0;
        int end = MIN_SPACING * 20;
        List<WindowPacker.PackingCandidate> pool = List.of(
                candidate(KEY_A, 1.0, MIN_SPACING, SHORT_COOLDOWN),
                candidate(KEY_B, 1.0, MIN_SPACING, SHORT_COOLDOWN)
        );
        OrderedPreferenceSelectionStrategy strategy = new OrderedPreferenceSelectionStrategy(List.of(KEY_B, KEY_A, KEY_A));

        // Act
        List<WindowPacker.PackedSlot> result = WindowPacker.pack(pool, start, end, 50, strategy, new Random(0L), 1.0);

        // Assert — B still ranks before A, exactly as if the duplicate were never appended
        assertTrue(result.size() >= 2);
        assertEquals(KEY_B, result.get(0).key());
        assertEquals(KEY_A, result.get(1).key());
    }

    @Test
    void pack_orderedStrategy_orderLongerThanPoolIgnoresExtraKeys() {
        // Arrange — the supplied order has far more keys than the pool contains
        int start = 0;
        int end = MIN_SPACING * 20;
        List<WindowPacker.PackingCandidate> pool = List.of(
                candidate(KEY_A, 1.0, MIN_SPACING, SHORT_COOLDOWN)
        );
        BehaviorKey extra1 = BehaviorKey.of("test_extra_1");
        BehaviorKey extra2 = BehaviorKey.of("test_extra_2");
        OrderedPreferenceSelectionStrategy strategy =
                new OrderedPreferenceSelectionStrategy(List.of(extra1, extra2, KEY_A, KEY_B, KEY_C));

        // Act
        List<WindowPacker.PackedSlot> result = WindowPacker.pack(pool, start, end, 50, strategy, new Random(0L), 1.0);

        // Assert — only KEY_A can ever be emitted since it's the only pool member
        assertFalse(result.isEmpty());
        assertTrue(result.stream().allMatch(s -> s.key().equals(KEY_A)));
    }

    @RepeatedTest(20)
    void pack_packFactorBelowOneAdvancesCursorByFractionOfDuration() {
        // Arrange — packFactor 0.5 means each successful emit advances the cursor by half the
        // slot's duration (the over-packing mechanism, §4.4), not the full duration. A single-key
        // pool with a cooldown below MIN_SPACING removes cooldown as a confound, so every gap
        // between consecutive emits is attributable to the packFactor-scaled advance alone.
        int start = 0;
        int end = MIN_SPACING * 30;
        int duration = MIN_SPACING * 3;
        double packFactor = 0.5;
        int expectedAdvance = (int) Math.round(duration * packFactor);
        List<WindowPacker.PackingCandidate> pool = List.of(
                candidate(KEY_A, 1.0, duration, MINIMUM_COOLDOWN)
        );

        // Act
        List<WindowPacker.PackedSlot> result = WindowPacker.pack(pool, start, end, 50, WEIGHTED_RANDOM, new Random(), packFactor);

        // Assert
        assertTrue(result.size() >= 3, "Window should fit several over-packed slots");
        for (int i = 1; i < result.size(); i++) {
            int gap = result.get(i).startCivil() - result.get(i - 1).startCivil();
            assertEquals(expectedAdvance, gap,
                    "Cursor must advance by round(duration * packFactor) between consecutive emits");
        }
    }

    @RepeatedTest(20)
    void pack_packFactorAdvanceNeverGoesBelowMinimumSlotSpacing() {
        // Arrange — an aggressive packFactor would compute an advance far below the minimum
        // spacing floor; the floor must win regardless, so the packer never emits two slots of the
        // same key closer than MIN_SPACING apart even under heavy over-packing.
        int start = 0;
        int end = MIN_SPACING * 30;
        int duration = MIN_SPACING; // round(duration * 0.05) << MIN_SPACING
        double packFactor = 0.05;
        List<WindowPacker.PackingCandidate> pool = List.of(
                candidate(KEY_A, 1.0, duration, MINIMUM_COOLDOWN)
        );

        // Act
        List<WindowPacker.PackedSlot> result = WindowPacker.pack(pool, start, end, 50, WEIGHTED_RANDOM, new Random(), packFactor);

        // Assert
        assertTrue(result.size() >= 3);
        for (int i = 1; i < result.size(); i++) {
            int gap = result.get(i).startCivil() - result.get(i - 1).startCivil();
            assertEquals(MIN_SPACING, gap,
                    "Advance must be floored at MINIMUM_SLOT_SPACING_TICKS regardless of packFactor");
        }
    }

    @Test
    void pack_packFactorDoesNotShrinkTheFullDurationEligibilityCheck() {
        // Arrange — a candidate whose full duration does NOT fit the window must still be excluded
        // even at a tiny packFactor: packFactor only discounts the post-emit cursor advance, never
        // the "does it fit" eligibility check.
        int start = 0;
        int end = MIN_SPACING * 2;
        int duration = MIN_SPACING * 3; // never fits [start, end)
        List<WindowPacker.PackingCandidate> pool = List.of(
                candidate(KEY_A, 1.0, duration, MINIMUM_COOLDOWN)
        );

        // Act
        List<WindowPacker.PackedSlot> result = WindowPacker.pack(pool, start, end, 50, WEIGHTED_RANDOM, new Random(0L), 0.1);

        // Assert
        assertTrue(result.isEmpty(), "A candidate that never fits the window must never be emitted, regardless of packFactor");
    }

    @Test
    void pack_preSeededNextEligibleSuppressesSameKeyRegardlessOfCallOrder() {
        // Arrange — nextEligible seeds KEY_A ineligible until tick 5000, simulating a pin's
        // reserved cooldown window pre-seeded before fill runs (day-plan-llm-wiring-p4.md §2).
        // The SAME map is threaded across two separate pack() calls over disjoint windows, mirroring
        // how DayPlanComposer shares one baseline across every band/interval in a compose() call —
        // this is the "both directions" guarantee: the seed suppresses KEY_A in a call over an
        // EARLIER window just as much as a call over a LATER one, since it isn't reset in between.
        Map<BehaviorKey, Integer> nextEligible = new HashMap<>();
        nextEligible.put(KEY_A, 5000);
        List<WindowPacker.PackingCandidate> pool = List.of(candidate(KEY_A, 1.0, MIN_SPACING, MINIMUM_COOLDOWN));

        // Act — an "earlier" window entirely before the seeded tick
        List<WindowPacker.PackedSlot> earlierWindow =
                WindowPacker.pack(pool, 0, 4000, 50, WEIGHTED_RANDOM, new Random(0L), 1.0, nextEligible);
        // ...then a "later" window straddling the seeded tick, sharing the same map
        List<WindowPacker.PackedSlot> laterWindow =
                WindowPacker.pack(pool, 4500, 6000, 50, WEIGHTED_RANDOM, new Random(0L), 1.0, nextEligible);

        // Assert
        assertTrue(earlierWindow.isEmpty(),
                "KEY_A must not be eligible before the seeded tick, even in a call over an earlier window");
        assertFalse(laterWindow.isEmpty(), "KEY_A becomes eligible again once the cursor reaches the seeded tick");
        for (WindowPacker.PackedSlot slot : laterWindow) {
            assertTrue(slot.startCivil() >= 5000, "no KEY_A slot may start before the pre-seeded eligibility tick");
        }
    }

    @Test
    void pack_noBaselineOverloadIsEquivalentToAFreshEmptyMap() {
        // Arrange — the 7-arg overload must behave exactly as if an empty map were passed, which is
        // what keeps the pure-heuristic golden draw sequence unaffected by the new overload existing.
        List<WindowPacker.PackingCandidate> pool = List.of(candidate(KEY_A, 1.0, MIN_SPACING, SHORT_COOLDOWN));

        // Act
        List<WindowPacker.PackedSlot> viaMapOverload =
                WindowPacker.pack(pool, 0, MIN_SPACING * 10, 50, WEIGHTED_RANDOM, new Random(42L), 1.0, new HashMap<>());
        List<WindowPacker.PackedSlot> viaPlainOverload =
                WindowPacker.pack(pool, 0, MIN_SPACING * 10, 50, WEIGHTED_RANDOM, new Random(42L), 1.0);

        // Assert — identical seed, identical pool, identical draw sequence
        assertEquals(viaPlainOverload.size(), viaMapOverload.size());
        for (int i = 0; i < viaPlainOverload.size(); i++) {
            assertEquals(viaPlainOverload.get(i).startCivil(), viaMapOverload.get(i).startCivil());
            assertEquals(viaPlainOverload.get(i).key(), viaMapOverload.get(i).key());
        }
    }

    private static WindowPacker.PackingCandidate candidate(BehaviorKey key, double weight,
                                                           int durationTicks, CooldownRange cooldown) {
        return new WindowPacker.PackingCandidate(key, weight, durationTicks, cooldown);
    }

}
