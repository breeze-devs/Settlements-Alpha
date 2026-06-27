package dev.breezes.settlements.domain.ai.memory;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Minecraft-free coordinate extractor for longs packed with {@code BlockPos.asLong()}.
 * <p>
 * Bit layout (same as vanilla {@code BlockPos}):
 * <ul>
 *   <li>X — bits [63..38], 26 bits, sign-extended</li>
 *   <li>Z — bits [37..12], 26 bits, sign-extended</li>
 *   <li>Y — bits [11..0], 12 bits, sign-extended</li>
 * </ul>
 * Centralizing these shifts eliminates the divergent hand-rolled copies that previously
 * existed in {@link SiteScorer}, {@link ConfirmedAbsenceRegion}, and elsewhere — one of
 * which had X and Z swapped.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class PackedPos {

    private static final int COORDS_BITS = 26;
    private static final int X_OFFSET = 38; // X starts at bit 38
    private static final int Z_OFFSET = 12; // Z starts at bit 12
    private static final int Y_BITS = 12;   // Y occupies the lowest 12 bits

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

}
