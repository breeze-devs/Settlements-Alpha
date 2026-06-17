package dev.breezes.settlements.domain.ai.knowledge;

import dev.breezes.settlements.domain.ai.observation.ObservationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the Phase 6 corroboration path in {@link VillagerKnowledgeStore}.
 * Covers the dedupe branch enhancement: same-source duplicate vs. independent corroboration
 * vs. hop-cap rejection. No Minecraft types are used.
 */
class VillagerKnowledgeStoreCorroborationTest {

    private VillagerKnowledgeStore store;

    @BeforeEach
    void setUp() {
        this.store = new VillagerKnowledgeStore();
    }

    // -------------------------------------------------------------------------
    // admit() — corroboration integrated into the live dedupe branch
    // -------------------------------------------------------------------------

    @Test
    void admit_returnsFalse_andCorroborates_whenDifferentSourceSharesSameOrigin() {
        // Arrange: first entry admitted from source A
        UUID originId = UUID.randomUUID();
        UUID sourceA = UUID.randomUUID();
        UUID sourceB = UUID.randomUUID();

        KnowledgeEntry firstEntry = hearsayEntry(originId, sourceA, 1, 2.0f);
        KnowledgeEntry corroborator = hearsayEntry(originId, sourceB, 1, 2.0f);

        this.store.admit(firstEntry);
        float weightBefore = this.store.findByOriginId(originId).get().getWeight();

        // Act: second entry from a different source with the same origin-id
        AdmitResult result = this.store.admit(corroborator);

        // Assert: entry is NOT stored as a new entry but weight is bumped
        assertEquals(AdmitResult.CORROBORATED_EXISTING, result, "Independent source should return CORROBORATED_EXISTING");
        assertEquals(1, this.store.size(), "Store size must not grow for a corroboration");

        float weightAfter = this.store.findByOriginId(originId).get().getWeight();
        assertTrue(weightAfter > weightBefore,
                "Corroboration should bump the existing entry's weight, before=" + weightBefore + " after=" + weightAfter);
    }

    @Test
    void admit_returnsFalse_andDoesNotCorroborate_whenSameSourceSharesSameOrigin() {
        // Arrange: same source shares the same origin-id twice (pure duplicate)
        UUID originId = UUID.randomUUID();
        UUID sourceA = UUID.randomUUID();

        KnowledgeEntry first = hearsayEntry(originId, sourceA, 1, 2.0f);
        KnowledgeEntry duplicate = hearsayEntry(originId, sourceA, 1, 2.0f);

        this.store.admit(first);
        float weightBefore = this.store.findByOriginId(originId).get().getWeight();
        int countBefore = this.store.findByOriginId(originId).get().getCorroborationCount();

        // Act
        AdmitResult result = this.store.admit(duplicate);

        // Assert: pure no-op — same source, no corroboration
        assertEquals(AdmitResult.IGNORED_DUPLICATE, result);
        assertEquals(1, this.store.size());

        float weightAfter = this.store.findByOriginId(originId).get().getWeight();
        int countAfter = this.store.findByOriginId(originId).get().getCorroborationCount();

        assertEquals(weightBefore, weightAfter, 0.001f, "Weight must not change for a same-source duplicate");
        assertEquals(countBefore, countAfter, "Corroboration count must not change for a same-source duplicate");
    }

    @Test
    void admit_rejectsBeyondHopCap_inCorroborationPath() {
        // Arrange: existing first entry; corroborator exceeds hop cap
        UUID originId = UUID.randomUUID();
        UUID sourceA = UUID.randomUUID();
        UUID sourceB = UUID.randomUUID();

        this.store.admit(hearsayEntry(originId, sourceA, 1, 2.0f));

        KnowledgeEntry overCap = hearsayEntry(originId, sourceB, KnowledgeEntry.MAX_HOP_COUNT + 1, 2.0f);

        // Act
        AdmitResult result = this.store.admit(overCap);

        // Assert: hop cap rejected; no corroboration applied
        assertEquals(AdmitResult.REJECTED_HOP_CAP, result);
        assertEquals(1, this.store.size());
        assertEquals(0, this.store.findByOriginId(originId).get().getCorroborationCount());
    }

    @Test
    void corroboration_incrementsCountPerIndependentSource() {
        // Arrange
        UUID originId = UUID.randomUUID();
        UUID sourceA = UUID.randomUUID();
        UUID sourceB = UUID.randomUUID();
        UUID sourceC = UUID.randomUUID();

        this.store.admit(hearsayEntry(originId, sourceA, 1, 2.0f));

        // Act: two independent corroborations
        this.store.admit(hearsayEntry(originId, sourceB, 1, 2.0f));
        this.store.admit(hearsayEntry(originId, sourceC, 1, 2.0f));

        // Assert
        KnowledgeEntry stored = this.store.findByOriginId(originId).get();
        assertEquals(2, stored.getCorroborationCount(),
                "Should have two independent corroborations");
    }

    @Test
    void corroboration_weightNeverExceedsTwiceOriginal_afterManyCorroborations() {
        // Arrange: original weight is 2.0, so the cap must be 4.0 regardless of corroboration count.
        // With CORROBORATION_BUMP = 0.1 and the old bug (cap = current * 2), weight would grow
        // unboundedly because the ceiling rose with every bump.
        UUID originId = UUID.randomUUID();
        UUID sourceA = UUID.randomUUID();
        float originalWeight = 2.0f;

        this.store.admit(hearsayEntry(originId, sourceA, 1, originalWeight));

        // Act: corroborate 200 times with unique sources — far more than enough to exceed 2× if the
        // cap were computed against the current weight instead of the original weight.
        for (int i = 0; i < 200; i++) {
            this.store.admit(hearsayEntry(originId, UUID.randomUUID(), 1, originalWeight));
        }

        // Assert
        KnowledgeEntry stored = this.store.findByOriginId(originId).get();
        float cap = originalWeight * 2.0f;
        assertTrue(stored.getWeight() <= cap,
                "Weight must never exceed 2× the original weight; expected ≤ " + cap + " but was " + stored.getWeight());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static KnowledgeEntry hearsayEntry(UUID originId, UUID sourceId, int hop, float weight) {
        // Build via fromDirectObservation + fromHearsay to avoid constructor coupling
        KnowledgeEntry base = KnowledgeEntry.fromDirectObservation(
                originId,
                "tip about resources",
                ObservationType.RESOURCE,
                100L,
                100L,
                null,
                Map.of(),
                weight);

        // Simulate hop-by-hop to reach desired hop count
        KnowledgeEntry hearsay = base;
        for (int i = 0; i < hop; i++) {
            hearsay = KnowledgeEntry.fromHearsay(hearsay, sourceId, 200L, weight);
        }
        return hearsay;
    }

}
