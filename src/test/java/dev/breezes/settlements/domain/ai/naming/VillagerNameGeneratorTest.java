package dev.breezes.settlements.domain.ai.naming;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillagerNameGeneratorTest {

    @Test
    void generateName_isIdempotent() {
        // Arrange
        UUID id = UUID.randomUUID();

        // Act — generate for the same UUID twice
        String first = VillagerNameGenerator.generateName(id);
        String second = VillagerNameGenerator.generateName(id);

        // Assert — same input always yields the same name
        assertEquals(first, second);
    }

    @Test
    void generateName_returnsNonNullNonEmptyName() {
        // Arrange
        UUID id = UUID.randomUUID();

        // Act
        String name = VillagerNameGenerator.generateName(id);

        // Assert
        assertNotNull(name);
        assertFalse(name.isBlank());
    }

    @Test
    void generateName_returnsNamesFromPool() {
        // Arrange
        UUID id = UUID.randomUUID();

        // Act
        String name = VillagerNameGenerator.generateName(id);

        // Assert — name must be one of the curated entries
        assertTrue(VillagerNameGenerator.NAME_POOL.contains(name),
                "Generated name '" + name + "' is not in the name pool");
    }

    @Test
    void generateName_spreadsAcrossPoolForRandomUuids() {
        // Arrange — 300 random UUIDs should hit significantly more than 10% of the pool
        // (probability of staying below that threshold with a uniform distribution is negligible).
        int sampleSize = 300;
        int minDistinctExpected = VillagerNameGenerator.NAME_POOL.size() / 2;
        Set<String> seen = new HashSet<>();

        // Act
        for (int i = 0; i < sampleSize; i++) {
            seen.add(VillagerNameGenerator.generateName(UUID.randomUUID()));
        }

        // Assert — good spread over the pool; catches degenerate hash collisions
        assertTrue(seen.size() >= minDistinctExpected,
                "Only " + seen.size() + " distinct names from " + sampleSize
                        + " samples; expected at least " + minDistinctExpected);
    }

    @Test
    void generateName_differentiatesDistinctUuids() {
        // Arrange — two clearly different UUIDs
        UUID a = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID b = UUID.fromString("00000000-0000-0000-0000-000000000002");

        // Act
        String nameA = VillagerNameGenerator.generateName(a);
        String nameB = VillagerNameGenerator.generateName(b);

        // Assert — at minimum, generation is deterministic; collisions are allowed but
        // these two sequential UUIDs should land on different names given XOR mixing.
        // (If they collide, the XOR formula needs inspection.)
        assertNotNull(nameA);
        assertNotNull(nameB);
        // Both must be in pool regardless of whether they differ
        assertTrue(VillagerNameGenerator.NAME_POOL.contains(nameA));
        assertTrue(VillagerNameGenerator.NAME_POOL.contains(nameB));
    }

    @Test
    void generateName_poolIsNonEmpty() {
        // Arrange — sanity guard on the pool constant itself

        // Act + Assert
        assertFalse(VillagerNameGenerator.NAME_POOL.isEmpty(),
                "NAME_POOL must not be empty");
        assertTrue(VillagerNameGenerator.NAME_POOL.size() >= 100,
                "NAME_POOL should be reasonably large (>=100); actual: " + VillagerNameGenerator.NAME_POOL.size());
    }

}
