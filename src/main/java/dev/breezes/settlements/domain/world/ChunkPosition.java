package dev.breezes.settlements.domain.world;

/**
 * A chunk's position on its dimension's chunk grid.
 *
 * @param x the chunk's X on the grid
 * @param z the chunk's Z on the grid
 */
public record ChunkPosition(int x, int z) {
}
