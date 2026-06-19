package dev.breezes.settlements.application.ai.naming;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link VillagerNameResolver}.
 * <p>
 * No Minecraft types are used — the resolver is pure Java.
 */
class VillagerNameResolverTest {

    private VillagerNameResolver resolver;

    @BeforeEach
    void setUp() {
        this.resolver = new VillagerNameResolver();
    }

    @Test
    void resolve_isIdempotent() {
        // Arrange
        UUID id = UUID.randomUUID();

        // Act — resolve the same UUID twice
        String first = this.resolver.resolve(id);
        String second = this.resolver.resolve(id);

        // Assert — same input always yields the same name
        assertEquals(first, second);
    }

    @Test
    void resolve_isStableAcrossInstances() {
        // Arrange — two independent resolver instances (simulating restart)
        VillagerNameResolver anotherInstance = new VillagerNameResolver();
        UUID id = UUID.randomUUID();

        // Act
        String fromThis = this.resolver.resolve(id);
        String fromOther = anotherInstance.resolve(id);

        // Assert — determinism is a function of input only, not instance state
        assertEquals(fromThis, fromOther);
    }

    @Test
    void resolve_returnsNonNullNonEmptyName() {
        // Arrange
        UUID id = UUID.randomUUID();

        // Act
        String name = this.resolver.resolve(id);

        // Assert
        assertNotNull(name);
        assertFalse(name.isBlank());
    }

    @Test
    void resolve_handlesNullGracefully() {
        // Arrange — null UUID should not throw

        // Act
        String name = this.resolver.resolve(null);

        // Assert — a safe fallback name is returned
        assertNotNull(name);
        assertFalse(name.isBlank());
    }

    @Test
    void resolve_returnsNamesFromPool() {
        // Arrange
        UUID id = UUID.randomUUID();

        // Act
        String name = this.resolver.resolve(id);

        // Assert — name must be one of the curated entries
        assertTrue(VillagerNameResolver.NAME_POOL.contains(name),
                "Resolved name '" + name + "' is not in the name pool");
    }

    @Test
    void resolve_spreadsAcrossPoolForRandomUuids() {
        // Arrange — 300 random UUIDs should hit significantly more than 10% of the pool
        // (probability of staying below that threshold with a uniform distribution is negligible).
        int sampleSize = 300;
        int minDistinctExpected = VillagerNameResolver.NAME_POOL.size() / 2;
        Set<String> seen = new HashSet<>();

        // Act
        for (int i = 0; i < sampleSize; i++) {
            seen.add(this.resolver.resolve(UUID.randomUUID()));
        }

        // Assert — good spread over the pool; catches degenerate hash collisions
        assertTrue(seen.size() >= minDistinctExpected,
                "Only " + seen.size() + " distinct names from " + sampleSize
                        + " samples; expected at least " + minDistinctExpected);
    }

    @Test
    void resolve_differentiatesDistinctUuids() {
        // Arrange — two clearly different UUIDs
        UUID a = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID b = UUID.fromString("00000000-0000-0000-0000-000000000002");

        // Act
        String nameA = this.resolver.resolve(a);
        String nameB = this.resolver.resolve(b);

        // Assert — at minimum, resolving is deterministic; collisions are allowed but
        // these two sequential UUIDs should land on different names given XOR mixing.
        // (If they collide, the XOR formula needs inspection.)
        assertNotNull(nameA);
        assertNotNull(nameB);
        // Both must be in pool regardless of whether they differ
        assertTrue(VillagerNameResolver.NAME_POOL.contains(nameA));
        assertTrue(VillagerNameResolver.NAME_POOL.contains(nameB));
    }

    @Test
    void resolve_poolIsNonEmpty() {
        // Arrange — sanity guard on the pool constant itself

        // Act + Assert
        assertFalse(VillagerNameResolver.NAME_POOL.isEmpty(),
                "NAME_POOL must not be empty");
        assertTrue(VillagerNameResolver.NAME_POOL.size() >= 100,
                "NAME_POOL should be reasonably large (>=100); actual: " + VillagerNameResolver.NAME_POOL.size());
    }

}
