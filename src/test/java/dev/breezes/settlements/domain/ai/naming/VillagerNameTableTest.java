package dev.breezes.settlements.domain.ai.naming;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link VillagerNameTable}.
 * <p>
 * No Minecraft types are used — the table is pure Java.
 */
class VillagerNameTableTest {

    private static final UUID VILLAGER_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID OTHER_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

    private VillagerNameTable table;

    @BeforeEach
    void setUp() {
        this.table = new VillagerNameTable();
    }

    @Test
    void resolve_unknownUuid_fallsBackToGenerator() {
        // Arrange — VILLAGER_ID was never upserted

        // Act
        String resolved = this.table.resolve(VILLAGER_ID);

        // Assert — fallback is exactly what the generator would have produced
        assertEquals(VillagerNameGenerator.generateName(VILLAGER_ID), resolved);
    }

    @Test
    void resolve_knownUuid_returnsStoredName() {
        // Arrange
        this.table.upsert(VILLAGER_ID, "Alderman Finn");

        // Act
        String resolved = this.table.resolve(VILLAGER_ID);

        // Assert — stored name wins over the generator fallback
        assertEquals("Alderman Finn", resolved);
    }

    @Test
    void upsert_newEntry_marksDirty() {
        // Arrange — table starts clean

        // Act
        this.table.upsert(VILLAGER_ID, "Finn");

        // Assert
        assertTrue(this.table.isDirty());
    }

    @Test
    void upsert_sameValueAgain_doesNotReMarkDirty() {
        // Arrange
        this.table.upsert(VILLAGER_ID, "Finn");
        this.table.clearDirty();

        // Act — upsert the exact same name a second time
        this.table.upsert(VILLAGER_ID, "Finn");

        // Assert — no actual content change occurred
        assertFalse(this.table.isDirty());
    }

    @Test
    void upsert_overwritesWithDifferentValue_marksDirtyAndReturnsNewName() {
        // Arrange
        this.table.upsert(VILLAGER_ID, "Finn");
        this.table.clearDirty();

        // Act
        this.table.upsert(VILLAGER_ID, "Renamed Finn");

        // Assert
        assertTrue(this.table.isDirty());
        assertEquals("Renamed Finn", this.table.resolve(VILLAGER_ID));
    }

    @Test
    void remove_existingEntry_marksDirtyAndFallsBackToGenerator() {
        // Arrange
        this.table.upsert(VILLAGER_ID, "Finn");
        this.table.clearDirty();

        // Act
        this.table.remove(VILLAGER_ID);

        // Assert
        assertTrue(this.table.isDirty());
        assertEquals(VillagerNameGenerator.generateName(VILLAGER_ID), this.table.resolve(VILLAGER_ID));
    }

    @Test
    void remove_absentEntry_doesNotMarkDirty() {
        // Arrange — VILLAGER_ID was never added; OTHER_ID is unrelated noise
        this.table.upsert(OTHER_ID, "Someone Else");
        this.table.clearDirty();

        // Act
        this.table.remove(VILLAGER_ID);

        // Assert — removing a UUID with no entry is not a content change
        assertFalse(this.table.isDirty());
    }

    @Test
    void clearDirty_resetsFlag() {
        // Arrange
        this.table.upsert(VILLAGER_ID, "Finn");

        // Act
        this.table.clearDirty();

        // Assert
        assertFalse(this.table.isDirty());
    }

    @Test
    void constructor_withInitialEntries_seedsWithoutMarkingDirty() {
        // Arrange — simulates the persistence shell reconstructing the table from NBT on load
        VillagerNameTable seeded = new VillagerNameTable(Map.of(VILLAGER_ID, "Finn"));

        // Act + Assert — a freshly-loaded table is not "dirty"; nothing changed, it was restored
        assertFalse(seeded.isDirty());
        assertEquals("Finn", seeded.resolve(VILLAGER_ID));
    }

    @Test
    void entriesView_reflectsCurrentContentOnly() {
        // Arrange
        this.table.upsert(VILLAGER_ID, "Finn");
        this.table.upsert(OTHER_ID, "Astrid");
        this.table.remove(OTHER_ID);

        // Act
        Map<UUID, String> entries = this.table.entriesView();

        // Assert — removed entry is gone, remaining entry is present
        assertEquals(1, entries.size());
        assertEquals("Finn", entries.get(VILLAGER_ID));
    }

}
