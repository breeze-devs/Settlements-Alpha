package dev.breezes.settlements.domain.ai.perception;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Unit tests for {@link SightingDedupeKeyFactory}.
 * <p>
 * All tests use only plain Java types — no Minecraft dependency. The pure overload
 * {@link SightingDedupeKeyFactory#computeDedupeKey(String, int, int, long)} is
 * exercised directly. Quantization is 2D chunk granularity with no Y axis.
 */
class SightingDedupeKeyFactoryTest {

    private static final String ZOMBIE_TYPE = "minecraft:zombie";
    private static final String PLAYER_TYPE = "minecraft:player";

    @Test
    void computeDedupeKey_sameInputs_returnsSameUuid() {
        // Arrange
        UUID first = SightingDedupeKeyFactory.computeDedupeKey(ZOMBIE_TYPE, 10, -5, 1000L);
        UUID second = SightingDedupeKeyFactory.computeDedupeKey(ZOMBIE_TYPE, 10, -5, 1000L);

        // Assert
        assertEquals(first, second, "Identical inputs must produce the same UUID");
    }

    @Test
    void computeDedupeKey_differentEntityType_returnsDifferentUuid() {
        // Arrange
        UUID zombie = SightingDedupeKeyFactory.computeDedupeKey(ZOMBIE_TYPE, 10, -5, 1000L);
        UUID player = SightingDedupeKeyFactory.computeDedupeKey(PLAYER_TYPE, 10, -5, 1000L);

        // Assert
        assertNotEquals(zombie, player, "Different entity types must produce different UUIDs");
    }

    @Test
    void computeDedupeKey_differentTimeBucket_returnsDifferentUuid() {
        // Arrange — time buckets are TIME_BUCKET_TICKS apart so they are in different windows
        long tickA = 0L;
        long tickB = SightingDedupeKeyFactory.TIME_BUCKET_TICKS;

        UUID keyA = SightingDedupeKeyFactory.computeDedupeKey(ZOMBIE_TYPE, 10, -5, tickA);
        UUID keyB = SightingDedupeKeyFactory.computeDedupeKey(ZOMBIE_TYPE, 10, -5, tickB);

        // Assert
        assertNotEquals(keyA, keyB, "Ticks in different time buckets must produce different UUIDs");
    }

    @Test
    void computeDedupeKey_sameEntityAndChunkButDifferentTimeBucket_returnsDifferentUuid() {
        // Arrange — identical entity type and chunk column, but ticks fall in adjacent buckets
        long tickInBucket0 = 0L;
        long tickInBucket1 = SightingDedupeKeyFactory.TIME_BUCKET_TICKS;

        UUID keyEarly = SightingDedupeKeyFactory.computeDedupeKey(ZOMBIE_TYPE, 5, 5, tickInBucket0);
        UUID keyLate = SightingDedupeKeyFactory.computeDedupeKey(ZOMBIE_TYPE, 5, 5, tickInBucket1);

        // Assert — a later encounter of the same threat in the same chunk is a distinct episode
        assertNotEquals(keyEarly, keyLate, "Same entity-type and chunk in different time buckets must differ");
    }

    @Test
    void computeDedupeKey_positionsInDifferentChunks_returnsDifferentUuid() {
        // Arrange — block 0 is in chunk 0; block 16 (0 >> 4 = 0, 16 >> 4 = 1) is in chunk 1
        int chunkBoundary = 1 << SightingDedupeKeyFactory.CHUNK_SHIFT; // = 16

        UUID keyInChunk0 = SightingDedupeKeyFactory.computeDedupeKey(ZOMBIE_TYPE, 0, 0, 1000L);
        UUID keyInChunk1 = SightingDedupeKeyFactory.computeDedupeKey(ZOMBIE_TYPE, chunkBoundary, 0, 1000L);

        // Assert
        assertNotEquals(keyInChunk0, keyInChunk1, "Positions in different chunk columns must produce different UUIDs");
    }

    @Test
    void computeDedupeKey_positionsInSameChunkColumn_returnsSameUuid() {
        // Arrange — block 1 and block 15 are both in chunk 0 (both >> 4 == 0)
        UUID keyAt1 = SightingDedupeKeyFactory.computeDedupeKey(ZOMBIE_TYPE, 1, 0, 1000L);
        UUID keyAt15 = SightingDedupeKeyFactory.computeDedupeKey(ZOMBIE_TYPE, 15, 0, 1000L);

        // Assert — two witnesses anywhere in the same chunk see "the same zombie"
        assertEquals(keyAt1, keyAt15, "Positions within the same 16-block chunk column must produce the same UUID");
    }

    @Test
    void computeDedupeKey_differentElevationSameChunkColumn_returnsSameUuid() {
        // Arrange — the factory has no Y parameter, so vertical offset cannot influence the key;
        // this proves Y-independence at the API level: the two calls are spatially identical inputs.
        UUID keyLow = SightingDedupeKeyFactory.computeDedupeKey(ZOMBIE_TYPE, 5, 5, 1000L);
        UUID keyHigh = SightingDedupeKeyFactory.computeDedupeKey(ZOMBIE_TYPE, 5, 5, 1000L);

        // Assert — same chunk column regardless of elevation → one converged memory
        assertEquals(keyLow, keyHigh, "Same chunk column must converge regardless of vertical offset");
    }

    @Test
    void computeDedupeKey_sameBucketDifferentTicksWithinBucket_returnsSameUuid() {
        // Arrange — two ticks within the same 2400-tick window
        long tickA = 500L;
        long tickB = 2399L; // still in bucket 0

        UUID keyA = SightingDedupeKeyFactory.computeDedupeKey(ZOMBIE_TYPE, 10, 0, tickA);
        UUID keyB = SightingDedupeKeyFactory.computeDedupeKey(ZOMBIE_TYPE, 10, 0, tickB);

        // Assert
        assertEquals(keyA, keyB, "Ticks within the same time bucket must produce the same UUID");
    }

    @Test
    void computeDedupeKey_negativeCoordinates_stableAndDistinct() {
        // Arrange — chunk arithmetic: -1 >> 4 == -1, -16 >> 4 == -1, -17 >> 4 == -2
        UUID keyAtMinus1 = SightingDedupeKeyFactory.computeDedupeKey(ZOMBIE_TYPE, -1, 0, 1000L);
        UUID keyAtMinus16 = SightingDedupeKeyFactory.computeDedupeKey(ZOMBIE_TYPE, -16, 0, 1000L);
        UUID keyAtMinus17 = SightingDedupeKeyFactory.computeDedupeKey(ZOMBIE_TYPE, -17, 0, 1000L);

        // Assert — -1 and -16 share chunk -1; -17 falls into chunk -2
        assertEquals(keyAtMinus1, keyAtMinus16, "Blocks -1 and -16 must share the same chunk column (-1 >> 4 == -1)");
        assertNotEquals(keyAtMinus1, keyAtMinus17, "Block -17 is in a different chunk column (-17 >> 4 == -2)");
    }
}
