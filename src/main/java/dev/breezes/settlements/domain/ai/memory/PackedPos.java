package dev.breezes.settlements.domain.ai.memory;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Minecraft-free packer/extractor for longs laid out identically to {@code BlockPos.asLong()}.
 * <p>
 * Bit layout (same as vanilla {@code BlockPos}):
 * <ul>
 *   <li>X — bits [63..38], 26 bits, sign-extended</li>
 *   <li>Z — bits [37..12], 26 bits, sign-extended</li>
 *   <li>Y — bits [11..0], 12 bits, sign-extended</li>
 * </ul>
 * Centralizing these shifts eliminates the divergent hand-rolled copies that previously
 * existed in {@link SiteScorer}, {@link ConfirmedAbsenceRegion}, and elsewhere — one of
 * which had X and Z swapped. {@link #asLong} is the inverse of {@link #x}/{@link #y}/{@link #z}
 * and is deliberately plain bit math rather than {@code BlockPos.asLong} so this class stays
 * usable from Minecraft-free domain code (e.g. {@code KnowledgeEntry}).
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class PackedPos {

    private static final int COORDS_BITS = 26;
    private static final int X_OFFSET = 38; // X starts at bit 38
    private static final int Z_OFFSET = 12; // Z starts at bit 12
    private static final int Y_BITS = 12;   // Y occupies the lowest 12 bits

    // Masks the low COORDS_BITS/Y_BITS bits of a shifted coordinate before OR-ing into the
    // packed long, so a negative input does not bleed sign bits into neighboring fields.
    private static final long COORDS_MASK = (1L << COORDS_BITS) - 1L;
    private static final long Y_MASK = (1L << Y_BITS) - 1L;

    /**
     * Extracts the sign-extended X coordinate from a packed {@code BlockPos.asLong()}.
     */
    public static int x(long packed) {
        // Left-shift to flush higher bits, then arithmetic right-shift to sign-extend.
        return (int) (packed << (64 - X_OFFSET - COORDS_BITS) >> (64 - COORDS_BITS));
    }

    /**
     * Extracts the sign-extended Z coordinate from a packed {@code BlockPos.asLong()}.
     */
    public static int z(long packed) {
        return (int) (packed << (64 - Z_OFFSET - COORDS_BITS) >> (64 - COORDS_BITS));
    }

    /**
     * Extracts the sign-extended Y coordinate from a packed {@code BlockPos.asLong()}.
     */
    public static int y(long packed) {
        // Y occupies the lowest 12 bits; sign-extend from bit 11.
        return (int) (packed << (64 - Y_BITS) >> (64 - Y_BITS));
    }

    /**
     * Packs a block coordinate into a single long using the same bit layout as
     * {@code BlockPos.asLong()} — the inverse of {@link #x}, {@link #y}, {@link #z}.
     * <p>
     * Values outside the vanilla-supported range (26-bit X/Z, 12-bit Y) silently wrap via the
     * mask, matching vanilla's own behavior rather than throwing.
     */
    public static long asLong(int x, int y, int z) {
        return ((long) x & COORDS_MASK) << X_OFFSET
                | ((long) z & COORDS_MASK) << Z_OFFSET
                | ((long) y & Y_MASK);
    }

}
