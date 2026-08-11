package dev.breezes.settlements.domain.farming;

import net.minecraft.core.BlockPos;

import javax.annotation.Nonnull;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * The rectangular cell grid one Cultivation Lily governs: the plane one block below the lily,
 * centered on its column, with the center water source excluded.
 * <p>
 * This is the authoritative zone-enumeration seam: any current or future behavior that needs to
 * discover a lily's cells should call {@link #streamCells()} rather than re-deriving the geometry.
 */
public record CultivationZone(@Nonnull BlockPos lilyPos, int halfExtentX, int halfExtentZ) {

    public static final int MIN_HALF_EXTENT = 1;
    /**
     * Matches the maximum vanilla hydration radius (Chebyshev 4 from the water source), so a
     * freshly placed lily's default zone is always fully hydrated without placing extra water.
     */
    public static final int MAX_HALF_EXTENT = 4;

    /**
     * Clamps out-of-range extents rather than rejecting them, so a value read from a corrupted save
     * converges to a valid zone instead of failing to load the block entity.
     */
    public CultivationZone {
        halfExtentX = Math.clamp(halfExtentX, MIN_HALF_EXTENT, MAX_HALF_EXTENT);
        halfExtentZ = Math.clamp(halfExtentZ, MIN_HALF_EXTENT, MAX_HALF_EXTENT);
    }

    public static CultivationZone atDefault(@Nonnull BlockPos lilyPos) {
        return new CultivationZone(lilyPos, MAX_HALF_EXTENT, MAX_HALF_EXTENT);
    }

    /**
     * Enumerates every candidate cell position at water Y, excluding the center water source.
     * Positions are constructed lazily.
     * <p>
     * Each position is a distinct immutable {@link BlockPos} rather than a reused cursor, because
     * callers are free to retain what this yields.
     */
    public Stream<BlockPos> streamCells() {
        int zoneY = this.lilyPos.getY() - 1;
        int centerX = this.lilyPos.getX();
        int centerZ = this.lilyPos.getZ();
        int widthZ = 2 * this.halfExtentZ + 1;
        int cellCount = (2 * this.halfExtentX + 1) * widthZ;

        return IntStream.range(0, cellCount)
                // The center cell (dx=0, dz=0) is the water source
                .filter(index -> index / widthZ != this.halfExtentX || index % widthZ != this.halfExtentZ)
                .mapToObj(index -> new BlockPos(
                        centerX + index / widthZ - this.halfExtentX,
                        zoneY,
                        centerZ + index % widthZ - this.halfExtentZ));
    }

    /**
     * A zone with the X half-extent advanced one step, wrapping from {@link #MAX_HALF_EXTENT} back to
     * {@link #MIN_HALF_EXTENT}.
     */
    public CultivationZone withNextHalfExtentX() {
        return new CultivationZone(this.lilyPos, wrapNext(this.halfExtentX), this.halfExtentZ);
    }

    /**
     * A zone with the Z half-extent advanced one step, wrapping from {@link #MAX_HALF_EXTENT} back to
     * {@link #MIN_HALF_EXTENT}.
     */
    public CultivationZone withNextHalfExtentZ() {
        return new CultivationZone(this.lilyPos, this.halfExtentX, wrapNext(this.halfExtentZ));
    }

    private static int wrapNext(int value) {
        return value >= MAX_HALF_EXTENT ? MIN_HALF_EXTENT : value + 1;
    }

}
