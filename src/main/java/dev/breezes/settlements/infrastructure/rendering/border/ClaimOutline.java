package dev.breezes.settlements.infrastructure.rendering.border;

import dev.breezes.settlements.domain.generation.model.geometry.Direction;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The outline of the settlement claims near a chunk: one straight edge along every chunk side where a claim meets
 * ground its own settlement does not claim.
 * <p>
 * A side facing a chunk this client does not hold gets no edge, since that chunk may carry the claim on.
 * Every edge stands the same distance inside its own chunk, whatever it faces, so along a border two settlements share,
 * each has its own edge and the two stand apart. Edges meet exactly at the corners where the outline turns.
 */
@ClientSide
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ClaimOutline {

    static final int CHUNK_WIDTH_BLOCKS = 16;
    private static final double INSET_BLOCKS = 0.3D;

    /**
     * Returns the edges of every claimed chunk this client holds within the radius of the center chunk, measured as the
     * straight-line distance between chunk coordinates.
     */
    static List<Edge> extract(@Nonnull ChunkClaims claims, int centerChunkX, int centerChunkZ, int radiusChunks) {
        List<Edge> edges = new ArrayList<>();
        int radiusSquared = radiusChunks * radiusChunks;
        for (int offsetX = -radiusChunks; offsetX <= radiusChunks; offsetX++) {
            for (int offsetZ = -radiusChunks; offsetZ <= radiusChunks; offsetZ++) {
                if (offsetX * offsetX + offsetZ * offsetZ > radiusSquared) {
                    continue;
                }
                int chunkX = centerChunkX + offsetX;
                int chunkZ = centerChunkZ + offsetZ;
                Optional<UUID> owner = claims.claimant(chunkX, chunkZ);
                if (owner.isEmpty()) {
                    continue;
                }
                for (Direction side : Direction.values()) {
                    addEdge(claims, chunkX, chunkZ, owner.get(), side, edges);
                }
            }
        }
        return edges;
    }

    private static void addEdge(@Nonnull ChunkClaims claims, int chunkX, int chunkZ, @Nonnull UUID owner,
                                @Nonnull Direction side, @Nonnull List<Edge> edges) {
        int facingX = chunkX + side.offsetX();
        int facingZ = chunkZ + side.offsetZ();
        if (!claims.holds(facingX, facingZ) || claimedBy(claims, facingX, facingZ, owner)) {
            return;
        }

        EdgeEnd start = endOf(claims, chunkX, chunkZ, owner, side, -1);
        EdgeEnd end = endOf(claims, chunkX, chunkZ, owner, side, 1);
        edges.add(new Edge(chunkX, chunkZ, side, owner,
                -start.extension(), CHUNK_WIDTH_BLOCKS + end.extension(), start.continues(), end.continues()));
    }

    /**
     * How the outline goes on past one end of an edge, judged from the chunk beside the edge's own chunk at that end
     * and the chunk diagonal to it, on the edge's outer side.
     *
     * @param direction -1 for the edge's start, 1 for its end
     */
    private static EdgeEnd endOf(@Nonnull ChunkClaims claims, int chunkX, int chunkZ, @Nonnull UUID owner,
                                 @Nonnull Direction side, int direction) {
        int besideX = chunkX + alongX(side) * direction;
        int besideZ = chunkZ + alongZ(side) * direction;
        if (!claims.holds(besideX, besideZ)) {
            return EdgeEnd.STOPS;
        }
        if (!claimedBy(claims, besideX, besideZ, owner)) {
            // Outer corner: the outline turns along this chunk's next side, whose edge stands the inset inside this
            // chunk, so this edge stops there
            return new EdgeEnd(-INSET_BLOCKS, false);
        }

        int diagonalX = besideX + side.offsetX();
        int diagonalZ = besideZ + side.offsetZ();
        if (!claims.holds(diagonalX, diagonalZ)) {
            return EdgeEnd.STOPS;
        }
        if (claimedBy(claims, diagonalX, diagonalZ, owner)) {
            // Inner corner: the outline turns onto the diagonal chunk's side, whose edge stands the inset inside the
            // diagonal chunk, past this chunk's corner
            return new EdgeEnd(INSET_BLOCKS, false);
        }
        return EdgeEnd.CONTINUES;
    }

    private static boolean claimedBy(@Nonnull ChunkClaims claims, int chunkX, int chunkZ, @Nonnull UUID owner) {
        return claims.claimant(chunkX, chunkZ).filter(owner::equals).isPresent();
    }

    private static int alongX(@Nonnull Direction side) {
        return side.offsetZ() != 0 ? 1 : 0;
    }

    private static int alongZ(@Nonnull Direction side) {
        return side.offsetX() != 0 ? 1 : 0;
    }

    /**
     * What this client knows of the claims on the surrounding chunks.
     */
    interface ChunkClaims {

        /**
         * Returns whether this client holds the chunk. The claim of a chunk it does not hold is unknown.
         */
        boolean holds(int chunkX, int chunkZ);

        /**
         * Returns the settlement claiming the chunk, or empty when no settlement claims it or this client does not hold
         * it.
         */
        Optional<UUID> claimant(int chunkX, int chunkZ);

    }

    /**
     * One straight edge of the outline, along one side of a claimed chunk.
     * <p>
     * A position along the edge is measured in blocks from the chunk's corner with the lowest coordinate on that side,
     * in the direction of increasing X for a north or south side and of increasing Z for an east or west side. The
     * edge's ground columns are the row of block columns inside the chunk along its side.
     *
     * @param chunkX             the claimed chunk's X
     * @param chunkZ             the claimed chunk's Z
     * @param side               the side of the chunk the edge runs along
     * @param owner              the settlement claiming the chunk
     * @param start              where the edge starts along the side: past the chunk's corner at an inner corner of the
     *                           outline, short of it at an outer corner, and on it otherwise
     * @param end                where the edge ends along the side, placed against the corner at that end the same way
     * @param continuesPastStart whether the outline goes on straight past the start, along the same side of the next
     *                           chunk
     * @param continuesPastEnd   whether the outline goes on straight past the end
     */
    record Edge(int chunkX, int chunkZ, @Nonnull Direction side, @Nonnull UUID owner,
                double start, double end, boolean continuesPastStart, boolean continuesPastEnd) {

        /**
         * Returns whether positions along the edge run along X rather than Z.
         */
        boolean runsAlongX() {
            return alongX(this.side) != 0;
        }

        /**
         * Returns the world coordinate the given position along the edge stands at: an X for an edge running along X,
         * otherwise a Z.
         */
        double worldAlong(double along) {
            return (this.runsAlongX() ? this.minBlockX() : this.minBlockZ()) + along;
        }

        /**
         * Returns the world coordinate of the vertical plane the edge stands in: a Z for an edge running along X,
         * otherwise an X.
         */
        double plane() {
            int min = this.runsAlongX() ? this.minBlockZ() : this.minBlockX();
            int outward = this.side.offsetX() + this.side.offsetZ();
            return outward > 0 ? min + CHUNK_WIDTH_BLOCKS - INSET_BLOCKS : min + INSET_BLOCKS;
        }

        /**
         * Returns the world X of the ground column at the given index along the edge. Index -1 and the chunk width name
         * the columns just past the start and end, in the neighboring chunks.
         */
        int columnX(int index) {
            int row = this.side.offsetX() > 0 ? this.minBlockX() + CHUNK_WIDTH_BLOCKS - 1 : this.minBlockX();
            return this.runsAlongX() ? this.minBlockX() + index : row;
        }

        /**
         * Returns the world Z of the ground column at the given index along the edge, as {@link #columnX} does for X.
         */
        int columnZ(int index) {
            int row = this.side.offsetZ() > 0 ? this.minBlockZ() + CHUNK_WIDTH_BLOCKS - 1 : this.minBlockZ();
            return this.runsAlongX() ? row : this.minBlockZ() + index;
        }

        private int minBlockX() {
            return this.chunkX * CHUNK_WIDTH_BLOCKS;
        }

        private int minBlockZ() {
            return this.chunkZ * CHUNK_WIDTH_BLOCKS;
        }

    }

    /**
     * How the outline goes on past one end of an edge.
     *
     * @param extension how far the edge reaches past its chunk's corner, in blocks; negative when it stops short of it
     * @param continues whether the outline goes on straight past the corner
     */
    private record EdgeEnd(double extension, boolean continues) {

        /**
         * The outline is not known past the corner, so the edge stops at it.
         */
        static final EdgeEnd STOPS = new EdgeEnd(0.0D, false);

        static final EdgeEnd CONTINUES = new EdgeEnd(0.0D, true);

    }

}
