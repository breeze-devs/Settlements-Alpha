package dev.breezes.settlements.infrastructure.rendering.border;

import dev.breezes.settlements.domain.generation.model.geometry.Direction;
import dev.breezes.settlements.infrastructure.rendering.border.ClaimOutline.Edge;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaimOutlineTest {

    private static final UUID A = new UUID(0L, 1L);
    private static final UUID B = new UUID(0L, 2L);

    /**
     * The test's own outline radius, in chunks, and the wider radius of chunks the client holds around the origin.
     */
    private static final int RADIUS = 3;
    private static final int HELD_RADIUS = RADIUS + 2;

    private static final double EPSILON = 1e-9;

    @Test
    void extract_tracesTheRegionPerimeter_andNoSideBetweenItsOwnChunks() {
        // Arrange
        ClientChunks chunks = ClientChunks.heldWithin(HELD_RADIUS);
        for (int chunkX = -1; chunkX <= 1; chunkX++) {
            for (int chunkZ = -1; chunkZ <= 1; chunkZ++) {
                chunks.claim(A, chunkX, chunkZ);
            }
        }

        // Act
        List<Edge> edges = ClaimOutline.extract(chunks, 0, 0, RADIUS);

        // Assert
        Set<String> expected = Set.of(
                "-1,-1,NORTH", "0,-1,NORTH", "1,-1,NORTH",
                "-1,1,SOUTH", "0,1,SOUTH", "1,1,SOUTH",
                "-1,-1,WEST", "-1,0,WEST", "-1,1,WEST",
                "1,-1,EAST", "1,0,EAST", "1,1,EAST");
        assertEquals(expected, sides(edges));
        assertTrue(edges.stream().allMatch(edge -> edge.owner().equals(A)));
    }

    @Test
    void extract_drawsNoEdgeTowardAChunkTheClientDoesNotHold() {
        // Arrange: the claim goes on east into a chunk this client has not loaded
        ClientChunks chunks = ClientChunks.heldWithin(HELD_RADIUS)
                .claim(A, 0, 0)
                .claim(A, 1, 0)
                .unload(1, 0);

        // Act
        List<Edge> edges = ClaimOutline.extract(chunks, 0, 0, RADIUS);

        // Assert
        assertTrue(edgeOn(edges, 0, 0, Direction.EAST).isEmpty());
        assertTrue(edgeOn(edges, 0, 0, Direction.WEST).isPresent());
    }

    @Test
    void extract_standsTouchingSettlementsWallsApart_eachInsideItsOwnChunk() {
        // Arrange: A's chunk spans X 0 to 16, and B's chunk east of it spans 16 to 32
        ClientChunks chunks = ClientChunks.heldWithin(HELD_RADIUS)
                .claim(A, 0, 0)
                .claim(B, 1, 0);
        double sharedLine = ClaimOutline.CHUNK_WIDTH_BLOCKS;

        // Act
        List<Edge> edges = ClaimOutline.extract(chunks, 0, 0, RADIUS);

        // Assert
        Edge wallOfA = edgeOn(edges, 0, 0, Direction.EAST).orElseThrow();
        Edge wallOfB = edgeOn(edges, 1, 0, Direction.WEST).orElseThrow();
        assertEquals(A, wallOfA.owner());
        assertEquals(B, wallOfB.owner());
        assertTrue(wallOfA.plane() < sharedLine, "A's wall must stand inside A's chunk");
        assertTrue(wallOfB.plane() > sharedLine, "B's wall must stand inside B's chunk");
    }

    @Test
    void extract_meetsTheTurningWallExactly_atAnInnerCorner() {
        // Arrange: an L of three chunks, notched where chunk (1, 0) is left unclaimed
        ClientChunks chunks = ClientChunks.heldWithin(HELD_RADIUS)
                .claim(A, 0, 0)
                .claim(A, 0, 1)
                .claim(A, 1, 1);

        // Act
        List<Edge> edges = ClaimOutline.extract(chunks, 0, 0, RADIUS);

        // Assert: both walls facing the notch reach exactly to the other's plane, past their own chunk's corner
        Edge alongZ = edgeOn(edges, 0, 0, Direction.EAST).orElseThrow();
        Edge alongX = edgeOn(edges, 1, 1, Direction.NORTH).orElseThrow();
        assertEquals(alongX.plane(), alongZ.worldAlong(alongZ.end()), EPSILON);
        assertEquals(alongZ.plane(), alongX.worldAlong(alongX.start()), EPSILON);
    }

    @Test
    void extract_reachesBothTurningWallsIntoOneColumn_atAnInnerCorner() {
        // Arrange: the same notched L
        ClientChunks chunks = ClientChunks.heldWithin(HELD_RADIUS)
                .claim(A, 0, 0)
                .claim(A, 0, 1)
                .claim(A, 1, 1);
        int pastEnd = ClaimOutline.CHUNK_WIDTH_BLOCKS;

        // Act
        List<Edge> edges = ClaimOutline.extract(chunks, 0, 0, RADIUS);

        // Assert: the column past one wall's end is the column past the other's start, so both stand on its ground
        Edge alongZ = edgeOn(edges, 0, 0, Direction.EAST).orElseThrow();
        Edge alongX = edgeOn(edges, 1, 1, Direction.NORTH).orElseThrow();
        assertEquals(alongZ.columnX(pastEnd), alongX.columnX(-1));
        assertEquals(alongZ.columnZ(pastEnd), alongX.columnZ(-1));
    }

    @Test
    void extract_meetsTheTurningWallExactly_atAnOuterCorner() {
        // Arrange
        ClientChunks chunks = ClientChunks.heldWithin(HELD_RADIUS).claim(A, 0, 0);

        // Act
        List<Edge> edges = ClaimOutline.extract(chunks, 0, 0, RADIUS);

        // Assert: each wall stops exactly at the other's plane, short of their shared corner
        Edge north = edgeOn(edges, 0, 0, Direction.NORTH).orElseThrow();
        Edge west = edgeOn(edges, 0, 0, Direction.WEST).orElseThrow();
        assertEquals(west.plane(), north.worldAlong(north.start()), EPSILON);
        assertEquals(north.plane(), west.worldAlong(west.start()), EPSILON);
    }

    @Test
    void extract_marksAStraightRunAcrossAChunkLine() {
        // Arrange: two chunks side by side, whose north walls form one straight line
        ClientChunks chunks = ClientChunks.heldWithin(HELD_RADIUS)
                .claim(A, 0, 0)
                .claim(A, 1, 0);

        // Act
        List<Edge> edges = ClaimOutline.extract(chunks, 0, 0, RADIUS);

        // Assert
        Edge west = edgeOn(edges, 0, 0, Direction.NORTH).orElseThrow();
        Edge east = edgeOn(edges, 1, 0, Direction.NORTH).orElseThrow();
        assertTrue(west.continuesPastEnd());
        assertTrue(east.continuesPastStart());
        assertEquals(west.worldAlong(west.end()), east.worldAlong(east.start()), EPSILON);
        assertFalse(west.continuesPastStart(), "the outline turns at the west chunk's outer corner");
    }

    @Test
    void extract_keepsAStraightRunInOnePlane_whereWhatItFacesChanges() {
        // Arrange: the run faces B's claim north of the west chunk and unclaimed ground north of the east one
        ClientChunks chunks = ClientChunks.heldWithin(HELD_RADIUS)
                .claim(A, 0, 0)
                .claim(A, 1, 0)
                .claim(B, 0, -1);

        // Act
        List<Edge> edges = ClaimOutline.extract(chunks, 0, 0, RADIUS);

        // Assert
        Edge facingB = edgeOn(edges, 0, 0, Direction.NORTH).orElseThrow();
        Edge facingOpenGround = edgeOn(edges, 1, 0, Direction.NORTH).orElseThrow();
        assertEquals(facingB.plane(), facingOpenGround.plane(), EPSILON);
    }

    @ParameterizedTest(name = "{0} side")
    @CsvSource({
            // side, the line it runs on, the column at index 3, and the column just past the end
            "NORTH, 0, 3, 0, 16, 0",
            "SOUTH, 16, 3, 15, 16, 15",
            "WEST, 0, 0, 3, 0, 16",
            "EAST, 16, 15, 3, 15, 16",
    })
    void edge_standsOnItsSide_andReadsTheGroundRowInsideItsChunk(Direction side, double sideLine,
                                                                 int columnX, int columnZ, int pastEndX, int pastEndZ) {
        // Arrange: chunk (0, 0) spans 0 to 16 on both axes
        ClientChunks chunks = ClientChunks.heldWithin(HELD_RADIUS).claim(A, 0, 0);
        double halfChunk = ClaimOutline.CHUNK_WIDTH_BLOCKS / 2.0D;

        // Act
        Edge edge = edgeOn(ClaimOutline.extract(chunks, 0, 0, RADIUS), 0, 0, side).orElseThrow();

        // Assert
        assertTrue(Math.abs(edge.plane() - sideLine) < halfChunk, "the edge stands nearer its own side line");
        assertTrue(edge.plane() > 0.0D && edge.plane() < ClaimOutline.CHUNK_WIDTH_BLOCKS,
                "the edge stands inside its chunk");
        assertEquals(columnX, edge.columnX(3));
        assertEquals(columnZ, edge.columnZ(3));
        assertEquals(pastEndX, edge.columnX(ClaimOutline.CHUNK_WIDTH_BLOCKS));
        assertEquals(pastEndZ, edge.columnZ(ClaimOutline.CHUNK_WIDTH_BLOCKS));
    }

    @ParameterizedTest(name = "chunk ({0}, {1}) outlined: {2}")
    @CsvSource({
            // Along an axis, up to the radius and no further
            "3, 0, true",
            "4, 0, false",
            // Across a diagonal, by straight-line distance rather than by the square around the center
            "2, 2, true",
            "3, 2, false",
    })
    void extract_outlinesOnlyClaimsWithinTheRadius(int chunkX, int chunkZ, boolean outlined) {
        // Arrange
        ClientChunks chunks = ClientChunks.heldWithin(HELD_RADIUS).claim(A, chunkX, chunkZ);

        // Act
        List<Edge> edges = ClaimOutline.extract(chunks, 0, 0, RADIUS);

        // Assert
        assertEquals(outlined, !edges.isEmpty());
    }

    private static Optional<Edge> edgeOn(List<Edge> edges, int chunkX, int chunkZ, Direction side) {
        return edges.stream()
                .filter(edge -> edge.chunkX() == chunkX && edge.chunkZ() == chunkZ && edge.side() == side)
                .findFirst();
    }

    private static Set<String> sides(List<Edge> edges) {
        return edges.stream()
                .map(edge -> edge.chunkX() + "," + edge.chunkZ() + "," + edge.side())
                .collect(Collectors.toSet());
    }

    /**
     * What a client knows of the chunks around the origin. A chunk it has unloaded reads as unclaimed, as a level's own
     * claim lookup reads it, whatever the server holds there.
     */
    private static final class ClientChunks implements ClaimOutline.ChunkClaims {

        private final Set<Long> held = new HashSet<>();
        private final Map<Long, UUID> claimants = new HashMap<>();

        static ClientChunks heldWithin(int radius) {
            ClientChunks chunks = new ClientChunks();
            for (int chunkX = -radius; chunkX <= radius; chunkX++) {
                for (int chunkZ = -radius; chunkZ <= radius; chunkZ++) {
                    chunks.held.add(key(chunkX, chunkZ));
                }
            }
            return chunks;
        }

        ClientChunks claim(UUID owner, int chunkX, int chunkZ) {
            this.claimants.put(key(chunkX, chunkZ), owner);
            return this;
        }

        ClientChunks unload(int chunkX, int chunkZ) {
            this.held.remove(key(chunkX, chunkZ));
            return this;
        }

        @Override
        public boolean holds(int chunkX, int chunkZ) {
            return this.held.contains(key(chunkX, chunkZ));
        }

        @Override
        public Optional<UUID> claimant(int chunkX, int chunkZ) {
            return this.holds(chunkX, chunkZ)
                    ? Optional.ofNullable(this.claimants.get(key(chunkX, chunkZ)))
                    : Optional.empty();
        }

        private static long key(int chunkX, int chunkZ) {
            return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
        }

    }

}
