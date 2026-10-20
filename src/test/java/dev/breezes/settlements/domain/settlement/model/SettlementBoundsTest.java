package dev.breezes.settlements.domain.settlement.model;

import dev.breezes.settlements.domain.world.ChunkPosition;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementBoundsTest {

    private static final ChunkPosition CENTER = new ChunkPosition(6, -13);

    @Test
    void contains_onlyTheClaimedChunks() {
        // Arrange
        ChunkPosition east = new ChunkPosition(7, -13);
        SettlementBounds bounds = new SettlementBounds(CENTER, Set.of(CENTER, east));

        // Act, Assert
        assertTrue(bounds.contains(east));
        assertFalse(bounds.contains(new ChunkPosition(8, -13)));
    }

    @Test
    void acceptsAClaim_joinedAroundACorner() {
        // Arrange — an L of three chunks: the far end reaches the center only through the corner chunk
        Set<ChunkPosition> chunks = Set.of(CENTER, new ChunkPosition(7, -13), new ChunkPosition(7, -12));

        // Act, Assert
        assertDoesNotThrow(() -> new SettlementBounds(CENTER, chunks));
    }

    @Test
    void rejectsAClaim_withoutItsCenterChunk() {
        // Arrange
        Set<ChunkPosition> chunks = Set.of(new ChunkPosition(7, -13));

        // Act, Assert
        assertThrows(IllegalArgumentException.class, () -> new SettlementBounds(CENTER, chunks));
    }

    @Test
    void rejectsAClaim_whoseChunksTouchOnlyDiagonally() {
        // Arrange
        Set<ChunkPosition> chunks = Set.of(CENTER, new ChunkPosition(7, -12));

        // Act, Assert
        assertThrows(IllegalArgumentException.class, () -> new SettlementBounds(CENTER, chunks));
    }

    @Test
    void rejectsAClaim_splitIntoSeparateRegions() {
        // Arrange — a second region two chunks east, with a gap between
        Set<ChunkPosition> chunks = Set.of(CENTER, new ChunkPosition(8, -13), new ChunkPosition(9, -13));

        // Act, Assert
        assertThrows(IllegalArgumentException.class, () -> new SettlementBounds(CENTER, chunks));
    }

    @Test
    void centerFirst_listsTheCenterThenEachSquareRingOutward() {
        // Arrange — a 5x5 square claim: rings of 1, 8, and 16 chunks, where a ring-one corner is as far from the center
        // by edges as a ring-two side chunk
        Set<ChunkPosition> ringOne = square(1);
        ringOne.remove(CENTER);
        Set<ChunkPosition> ringTwo = square(2);
        ringTwo.removeAll(square(1));
        SettlementBounds bounds = new SettlementBounds(CENTER, square(2));

        // Act
        List<ChunkPosition> order = bounds.centerFirst();

        // Assert
        assertEquals(25, order.size());
        assertEquals(CENTER, order.getFirst());
        assertEquals(ringOne, Set.copyOf(order.subList(1, 9)));
        assertEquals(ringTwo, Set.copyOf(order.subList(9, 25)));
    }

    @Test
    void centerFirst_listsOnlyTheClaimedChunks_ofARingTheClaimFillsInPart() {
        // Arrange — a row east of the center that bends south at its end; the unclaimed chunks of rings one and two
        // must not appear
        ChunkPosition east = new ChunkPosition(7, -13);
        ChunkPosition farEast = new ChunkPosition(8, -13);
        ChunkPosition farEastSouth = new ChunkPosition(8, -12);
        SettlementBounds bounds = new SettlementBounds(CENTER, Set.of(CENTER, east, farEast, farEastSouth));

        // Act
        List<ChunkPosition> order = bounds.centerFirst();

        // Assert
        assertEquals(4, order.size());
        assertEquals(List.of(CENTER, east), order.subList(0, 2));
        assertEquals(Set.of(farEast, farEastSouth), Set.copyOf(order.subList(2, 4)));
    }

    @Test
    void clip_removesTheChunksClaimedElsewhere() {
        // Arrange — a square of four chunks whose far corner another settlement claims
        ChunkPosition east = new ChunkPosition(7, -13);
        ChunkPosition south = new ChunkPosition(6, -12);
        ChunkPosition southEast = new ChunkPosition(7, -12);
        SettlementBounds bounds = new SettlementBounds(CENTER, Set.of(CENTER, east, south, southEast));

        // Act
        Optional<SettlementBounds> clipped = bounds.clip(Set.of(southEast)::contains);

        // Assert
        assertEquals(Optional.of(new SettlementBounds(CENTER, Set.of(CENTER, east, south))), clipped);
    }

    @Test
    void clip_dropsThePartCutOffFromTheCenter() {
        // Arrange — a row of four chunks; another settlement claims the one east of the center, which leaves the far
        // east chunk joined to nothing
        ChunkPosition west = new ChunkPosition(5, -13);
        ChunkPosition east = new ChunkPosition(7, -13);
        ChunkPosition farEast = new ChunkPosition(8, -13);
        SettlementBounds bounds = new SettlementBounds(CENTER, Set.of(west, CENTER, east, farEast));

        // Act
        Optional<SettlementBounds> clipped = bounds.clip(Set.of(east)::contains);

        // Assert
        assertEquals(Optional.of(new SettlementBounds(CENTER, Set.of(west, CENTER))), clipped);
    }

    @Test
    void clip_returnsTheClaimItself_whenNoneOfItsChunksIsClaimedElsewhere() {
        // Arrange — another settlement claims the chunk just past the claim's east edge
        SettlementBounds bounds = new SettlementBounds(CENTER, Set.of(CENTER, new ChunkPosition(7, -13)));

        // Act
        Optional<SettlementBounds> clipped = bounds.clip(Set.of(new ChunkPosition(8, -13))::contains);

        // Assert
        assertTrue(clipped.isPresent());
        assertSame(bounds, clipped.get());
    }

    @Test
    void clip_refusesTheClaim_whenItsCenterIsClaimedElsewhere() {
        // Arrange
        SettlementBounds bounds = new SettlementBounds(CENTER, Set.of(CENTER, new ChunkPosition(7, -13)));

        // Act
        Optional<SettlementBounds> clipped = bounds.clip(Set.of(CENTER)::contains);

        // Assert
        assertEquals(Optional.empty(), clipped);
    }

    private static Set<ChunkPosition> square(int radius) {
        Set<ChunkPosition> chunks = new HashSet<>();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                chunks.add(new ChunkPosition(CENTER.x() + x, CENTER.z() + z));
            }
        }
        return chunks;
    }

}
