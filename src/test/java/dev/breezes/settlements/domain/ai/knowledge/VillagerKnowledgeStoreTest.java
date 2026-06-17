package dev.breezes.settlements.domain.ai.knowledge;

import dev.breezes.settlements.domain.ai.observation.ObservationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link VillagerKnowledgeStore}: Guard 1 (origin-id dedupe),
 * Guard 2 (hop cap), capacity eviction, and shareable-entry filtering.
 * No Minecraft types are used.
 */
class VillagerKnowledgeStoreTest {

    private VillagerKnowledgeStore store;

    @BeforeEach
    void setUp() {
        this.store = new VillagerKnowledgeStore();
    }

    // -------------------------------------------------------------------------
    // Guard 1 — dedupe on origin id
    // -------------------------------------------------------------------------

    @Test
    void admit_acceptsNewEntry() {
        // Arrange
        KnowledgeEntry entry = directEntry(UUID.randomUUID(), "ripe melons spotted", 2.5f);

        // Act
        AdmitResult result = this.store.admit(entry);

        // Assert
        assertEquals(AdmitResult.NEW_ENTRY, result);
        assertEquals(1, this.store.size());
    }

    @Test
    void admit_rejectsDuplicateOriginId() {
        // Arrange
        UUID originId = UUID.randomUUID();
        KnowledgeEntry first = directEntry(originId, "ripe melons spotted", 2.5f);
        KnowledgeEntry duplicate = directEntry(originId, "same fact via different path", 1.0f);

        // Act
        AdmitResult firstResult = this.store.admit(first);
        AdmitResult secondResult = this.store.admit(duplicate);

        // Assert – Guard 1: duplicate origin-id is rejected regardless of weight or content.
        // Both entries share the same source (null for direct observations) so this is IGNORED_DUPLICATE.
        assertEquals(AdmitResult.NEW_ENTRY, firstResult);
        assertEquals(AdmitResult.IGNORED_DUPLICATE, secondResult);
        assertEquals(1, this.store.size());
    }

    @Test
    void knows_trueAfterAdmission() {
        // Arrange
        UUID originId = UUID.randomUUID();
        this.store.admit(directEntry(originId, "zombie sighting", 3.0f));

        // Act & Assert
        assertTrue(this.store.knows(originId));
    }

    @Test
    void knows_falseForUnknownOriginId() {
        // Arrange & Act & Assert
        assertFalse(this.store.knows(UUID.randomUUID()));
    }

    // -------------------------------------------------------------------------
    // Guard 2 — hop cap
    // -------------------------------------------------------------------------

    @Test
    void admit_rejectsEntryBeyondHopCap() {
        // Arrange – entry at hop MAX_HOP_COUNT + 1 should be rejected
        KnowledgeEntry tooManyHops = hearsayEntry(UUID.randomUUID(), KnowledgeEntry.MAX_HOP_COUNT + 1);

        // Act
        AdmitResult result = this.store.admit(tooManyHops);

        // Assert – Guard 2: over-cap entries are never stored
        assertEquals(AdmitResult.REJECTED_HOP_CAP, result);
        assertTrue(this.store.isEmpty());
    }

    @Test
    void admit_acceptsEntryAtExactHopCap() {
        // Arrange – entry exactly at the cap is stored (but not re-shareable)
        KnowledgeEntry atCap = hearsayEntry(UUID.randomUUID(), KnowledgeEntry.MAX_HOP_COUNT);

        // Act
        AdmitResult result = this.store.admit(atCap);

        // Assert
        assertEquals(AdmitResult.NEW_ENTRY, result);
        assertEquals(1, this.store.size());
    }

    @Test
    void shareableEntries_excludesEntriesAtHopCap() {
        // Arrange
        UUID shareable1Id = UUID.randomUUID();
        UUID shareable2Id = UUID.randomUUID();
        UUID cappedId = UUID.randomUUID();

        this.store.admit(directEntry(shareable1Id, "first-hand fact", 3.0f));
        this.store.admit(hearsayEntry(shareable2Id, 1));        // hop 1 < cap → shareable
        this.store.admit(hearsayEntry(cappedId, KnowledgeEntry.MAX_HOP_COUNT)); // at cap → not shareable

        // Act
        List<KnowledgeEntry> shareable = this.store.shareableEntries();

        // Assert – only entries below the hop cap are returned
        assertEquals(2, shareable.size());
        assertTrue(shareable.stream().noneMatch(e -> e.getOriginObservationId().equals(cappedId)));
    }

    // -------------------------------------------------------------------------
    // Query
    // -------------------------------------------------------------------------

    @Test
    void findByOriginId_returnsEntryWhenPresent() {
        // Arrange
        UUID originId = UUID.randomUUID();
        KnowledgeEntry entry = directEntry(originId, "crop harvested", 2.0f);
        this.store.admit(entry);

        // Act
        Optional<KnowledgeEntry> found = this.store.findByOriginId(originId);

        // Assert
        assertTrue(found.isPresent());
        assertEquals("crop harvested", found.get().getContent());
    }

    @Test
    void findByOriginId_emptyWhenAbsent() {
        // Arrange & Act
        Optional<KnowledgeEntry> found = this.store.findByOriginId(UUID.randomUUID());

        // Assert
        assertTrue(found.isEmpty());
    }

    @Test
    void entriesView_reflectsAdmissionsWithoutRequerying() {
        // Arrange
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        this.store.admit(directEntry(firstId, "first", 1.0f));
        Collection<KnowledgeEntry> view = this.store.entriesView();

        // Act
        this.store.admit(directEntry(secondId, "second", 1.0f));

        // Assert
        assertEquals(2, view.size());
        assertTrue(view.stream().anyMatch(e -> e.getOriginObservationId().equals(firstId)));
        assertTrue(view.stream().anyMatch(e -> e.getOriginObservationId().equals(secondId)));
    }

    @Test
    void entriesView_rejectsStructuralMutation() {
        // Arrange
        KnowledgeEntry entry = directEntry(UUID.randomUUID(), "protected", 1.0f);
        this.store.admit(entry);
        Collection<KnowledgeEntry> view = this.store.entriesView();

        // Act & Assert
        assertThrows(UnsupportedOperationException.class, () -> view.remove(entry));
        assertEquals(1, this.store.size());
    }

    // -------------------------------------------------------------------------
    // Capacity eviction
    // -------------------------------------------------------------------------

    @Test
    void admit_evictsOldestWhenFull() {
        // Arrange – fill to capacity
        UUID firstId = UUID.randomUUID();
        this.store.admit(directEntry(firstId, "oldest entry", 1.0f));
        for (int i = 1; i < VillagerKnowledgeStore.MAX_ENTRIES; i++) {
            this.store.admit(directEntry(UUID.randomUUID(), "filler " + i, 1.0f));
        }
        assertEquals(VillagerKnowledgeStore.MAX_ENTRIES, this.store.size());

        // Act – one more entry pushes the oldest out
        UUID newId = UUID.randomUUID();
        AdmitResult result = this.store.admit(directEntry(newId, "newest entry", 1.0f));

        // Assert
        assertEquals(AdmitResult.NEW_ENTRY, result);
        assertEquals(VillagerKnowledgeStore.MAX_ENTRIES, this.store.size());
        assertFalse(this.store.knows(firstId), "Oldest entry should have been evicted");
        assertTrue(this.store.knows(newId), "Newest entry should be present");
    }

    @Test
    void admit_usesConfiguredCapacity() {
        // Arrange
        VillagerKnowledgeStore smallStore = new VillagerKnowledgeStore(2);
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        UUID thirdId = UUID.randomUUID();
        smallStore.admit(directEntry(firstId, "first", 1.0f));
        smallStore.admit(directEntry(secondId, "second", 1.0f));

        // Act
        smallStore.admit(directEntry(thirdId, "third", 1.0f));

        // Assert
        assertEquals(2, smallStore.maxEntries());
        assertEquals(2, smallStore.size());
        assertFalse(smallStore.knows(firstId));
        assertTrue(smallStore.knows(secondId));
        assertTrue(smallStore.knows(thirdId));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static KnowledgeEntry directEntry(UUID originId, String content, float weight) {
        return KnowledgeEntry.fromDirectObservation(
                originId,
                content,
                ObservationType.RESOURCE,
                100L,
                100L,
                null,
                Map.of(),
                weight);
    }

    private static KnowledgeEntry hearsayEntry(UUID originId, int hop) {
        KnowledgeEntry source = directEntry(originId, "original", 2.0f);
        // Build a hearsay entry by simulating fromHearsay hop-by-hop
        KnowledgeEntry current = source;
        for (int i = 0; i < hop; i++) {
            // We simulate by building the final hearsay entry with the desired hop count directly
            // rather than chaining (the factory would normally do hop - 1 then add 1)
            current = KnowledgeEntry.fromHearsay(current, UUID.randomUUID(), 200L, 1.0f);
        }
        return current;
    }

}
