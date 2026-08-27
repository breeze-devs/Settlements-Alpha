package dev.breezes.settlements.domain.ai.knowledge;

import dev.breezes.settlements.domain.ai.observation.ObservationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link VillagerKnowledgeStore}: origin-id dedupe, capacity eviction, and the
 * read views.
 */
class VillagerKnowledgeStoreTest {

    private VillagerKnowledgeStore store;

    @BeforeEach
    void setUp() {
        this.store = new VillagerKnowledgeStore();
    }

    @Test
    void admit_acceptsNewEntry() {
        // Arrange
        KnowledgeEntry entry = directEntry(UUID.randomUUID(), 2.5f);

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
        KnowledgeEntry first = directEntry(originId, 2.5f);
        KnowledgeEntry duplicate = directEntry(originId, 1.0f);

        // Act
        AdmitResult firstResult = this.store.admit(first);
        AdmitResult secondResult = this.store.admit(duplicate);

        // Assert – a duplicate origin-id is rejected regardless of weight or content
        assertEquals(AdmitResult.NEW_ENTRY, firstResult);
        assertEquals(AdmitResult.IGNORED_DUPLICATE, secondResult);
        assertEquals(1, this.store.size());
    }

    @Test
    void knows_trueAfterAdmission() {
        // Arrange
        UUID originId = UUID.randomUUID();
        this.store.admit(directEntry(originId, 3.0f));

        // Act & Assert
        assertTrue(this.store.knows(originId));
    }

    @Test
    void knows_falseForUnknownOriginId() {
        // Arrange & Act & Assert
        assertFalse(this.store.knows(UUID.randomUUID()));
    }

    @Test
    void findByOriginId_returnsEntryWhenPresent() {
        // Arrange
        UUID originId = UUID.randomUUID();
        KnowledgeEntry entry = directEntry(originId, 2.0f);
        this.store.admit(entry);

        // Act
        Optional<KnowledgeEntry> found = this.store.findByOriginId(originId);

        // Assert
        assertTrue(found.isPresent());
        assertEquals(originId, found.get().getOriginObservationId());
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
        this.store.admit(directEntry(firstId, 1.0f));
        Collection<KnowledgeEntry> view = this.store.entriesView();

        // Act
        this.store.admit(directEntry(secondId, 1.0f));

        // Assert
        assertEquals(2, view.size());
        assertTrue(view.stream().anyMatch(e -> e.getOriginObservationId().equals(firstId)));
        assertTrue(view.stream().anyMatch(e -> e.getOriginObservationId().equals(secondId)));
    }

    @Test
    void entriesView_rejectsStructuralMutation() {
        // Arrange
        KnowledgeEntry entry = directEntry(UUID.randomUUID(), 1.0f);
        this.store.admit(entry);
        Collection<KnowledgeEntry> view = this.store.entriesView();

        // Act & Assert
        assertThrows(UnsupportedOperationException.class, () -> view.remove(entry));
        assertEquals(1, this.store.size());
    }

    @Test
    void admit_evictsOldestWhenFull() {
        // Arrange – fill to capacity
        UUID firstId = UUID.randomUUID();
        this.store.admit(directEntry(firstId, 1.0f));
        for (int i = 1; i < VillagerKnowledgeStore.MAX_ENTRIES; i++) {
            this.store.admit(directEntry(UUID.randomUUID(), 1.0f));
        }
        assertEquals(VillagerKnowledgeStore.MAX_ENTRIES, this.store.size());

        // Act – one more entry pushes the oldest out
        UUID newId = UUID.randomUUID();
        AdmitResult result = this.store.admit(directEntry(newId, 1.0f));

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
        smallStore.admit(directEntry(firstId, 1.0f));
        smallStore.admit(directEntry(secondId, 1.0f));

        // Act
        smallStore.admit(directEntry(thirdId, 1.0f));

        // Assert
        assertEquals(2, smallStore.maxEntries());
        assertEquals(2, smallStore.size());
        assertFalse(smallStore.knows(firstId));
        assertTrue(smallStore.knows(secondId));
        assertTrue(smallStore.knows(thirdId));
    }

    private static KnowledgeEntry directEntry(UUID originId, float weight) {
        return KnowledgeEntry.fromDirectObservation(
                originId,
                ObservationType.RESOURCE,
                100L,
                100L,
                null,
                Map.of(),
                weight,
                null);
    }

}
