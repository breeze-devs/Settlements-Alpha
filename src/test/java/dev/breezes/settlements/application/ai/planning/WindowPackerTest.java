package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.catalog.CooldownRange;
import dev.breezes.settlements.domain.time.ClockTicks;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure (no Minecraft) invariant tests for {@link WindowPacker}.
 * <p>
 * All assertions must hold for any RNG outcome, so they are expressed as invariants
 * rather than exact positions. Tests are repeated to exercise the live RNG paths.
 */
class WindowPackerTest {

    private static final int MIN_SPACING = WindowPacker.MINIMUM_SLOT_SPACING_TICKS;

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

    // =========================================================================
    // Empty / degenerate inputs
    // =========================================================================

    @Test
    void pack_emptyPool_returnsEmpty() {
        // Arrange
        List<WindowPacker.PackingCandidate> pool = List.of();

        // Act
        List<WindowPacker.PackedSlot> result = WindowPacker.pack(pool, 0, MIN_SPACING * 10, 50);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void pack_nonPositiveWindow_returnsEmpty() {
        // Arrange
        List<WindowPacker.PackingCandidate> pool = List.of(candidate(KEY_A, 1.0, MIN_SPACING, MINIMUM_COOLDOWN));

        // Act — endLinear == startLinear
        List<WindowPacker.PackedSlot> sameEdge = WindowPacker.pack(pool, 100, 100, 50);
        // Act — endLinear < startLinear
        List<WindowPacker.PackedSlot> inverted = WindowPacker.pack(pool, 200, 100, 50);

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
        List<WindowPacker.PackedSlot> result = WindowPacker.pack(pool, 0, MIN_SPACING * 10, 50);

        // Assert
        assertTrue(result.isEmpty());
    }

    // =========================================================================
    // All slots fit within the window
    // =========================================================================

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
        List<WindowPacker.PackedSlot> result = WindowPacker.pack(pool, start, end, 50);

        // Assert — every slot's [startLinear, startLinear+duration) must fit within [start, end)
        for (WindowPacker.PackedSlot slot : result) {
            assertTrue(slot.startLinear() >= start,
                    "startLinear " + slot.startLinear() + " must be >= " + start);
            assertTrue(slot.startLinear() + slot.durationTicks() <= end,
                    "slot end " + (slot.startLinear() + slot.durationTicks()) + " must be <= " + end);
        }
    }

    // =========================================================================
    // Per-key spacing invariant
    // =========================================================================

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
        List<WindowPacker.PackedSlot> result = WindowPacker.pack(pool, start, end, 50);

        // Assert — consecutive appearances of the same key must be at least cooldown apart
        List<WindowPacker.PackedSlot> keyASlots = result.stream()
                .filter(s -> s.key().equals(KEY_A))
                .toList();

        for (int i = 1; i < keyASlots.size(); i++) {
            int gap = keyASlots.get(i).startLinear() - keyASlots.get(i - 1).startLinear();
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
        List<WindowPacker.PackedSlot> result = WindowPacker.pack(pool, start, end, 50);

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
        List<WindowPacker.PackedSlot> result = WindowPacker.pack(pool, start, end, 50);

        // Assert — can't appear more often than the window / cooldown allows
        int maxExpected = (end - start) / cooldownSpacing + 1;
        assertTrue(result.size() <= maxExpected,
                "Slot count " + result.size() + " exceeds max expected " + maxExpected);
    }

    // =========================================================================
    // Coverage invariant — every key appears at least once for diverse pools
    // =========================================================================

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
        List<WindowPacker.PackedSlot> result = WindowPacker.pack(pool, start, end, 50);

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
        List<WindowPacker.PackedSlot> result = WindowPacker.pack(pool, start, end, 50);

        // Assert — for each key, check consecutive appearances are >= cooldown apart
        Map<BehaviorKey, List<WindowPacker.PackedSlot>> byKey = result.stream()
                .collect(Collectors.groupingBy(WindowPacker.PackedSlot::key));

        for (Map.Entry<BehaviorKey, List<WindowPacker.PackedSlot>> entry : byKey.entrySet()) {
            List<WindowPacker.PackedSlot> keySlots = entry.getValue();
            for (int i = 1; i < keySlots.size(); i++) {
                int gap = keySlots.get(i).startLinear() - keySlots.get(i - 1).startLinear();
                assertTrue(gap >= MIN_SPACING,
                        "Key " + entry.getKey().id() + " slots " + (i - 1) + " and " + i
                                + " are only " + gap + " ticks apart (min=" + MIN_SPACING + ")");
            }
        }
    }

    // =========================================================================
    // Priority decrements with each emitted slot
    // =========================================================================

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
        List<WindowPacker.PackedSlot> result = WindowPacker.pack(pool, start, end, basePriority);

        // Assert — slots appear in emission order; first must have the highest priority
        assertFalse(result.isEmpty());
        assertEquals(basePriority, result.get(0).priority());

        for (int i = 1; i < result.size(); i++) {
            assertTrue(result.get(i).priority() <= result.get(i - 1).priority(),
                    "Priority must be non-increasing across emitted slots");
            assertTrue(result.get(i).priority() >= 0, "Priority must be non-negative");
        }
    }

    // =========================================================================
    // Zero-weight exclusion acts as rest-day suppression
    // =========================================================================

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
        List<WindowPacker.PackedSlot> result = WindowPacker.pack(pool, start, end, 50);

        // Assert
        assertFalse(result.isEmpty());
        assertTrue(result.stream().noneMatch(s -> s.key().equals(KEY_B)),
                "Zero-weight key must never appear in the output");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static WindowPacker.PackingCandidate candidate(BehaviorKey key, double weight,
                                                           int durationTicks, CooldownRange cooldown) {
        return new WindowPacker.PackingCandidate(key, weight, durationTicks, cooldown);
    }

}
