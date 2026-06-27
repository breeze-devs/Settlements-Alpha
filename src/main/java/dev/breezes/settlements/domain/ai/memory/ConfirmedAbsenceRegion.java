package dev.breezes.settlements.domain.ai.memory;

/**
 * Axis-aligned box in which the sensor authoritatively confirmed the absence of a resource this scan cycle.
 * Sites remembered inside this box but not re-confirmed are deleted.
 * <p>
 * Coordinate extraction from packed longs is delegated to {@link PackedPos}, which owns the single
 * canonical definition of the {@code BlockPos.asLong()} bit layout.
 */
public record ConfirmedAbsenceRegion(int minX, int minY, int minZ,
                                     int maxX, int maxY, int maxZ) {

    /**
     * Builds the region from an anchor block-position and the horizontal/vertical scan radii.
     * Derives from {@code complete = true} on a query result: the whole scan box was authoritative.
     */
    public static ConfirmedAbsenceRegion ofScanBox(int anchorX, int anchorY, int anchorZ,
                                                   int horizontalRadius, int verticalRadius) {
        return new ConfirmedAbsenceRegion(
                anchorX - horizontalRadius, anchorY - verticalRadius, anchorZ - horizontalRadius,
                anchorX + horizontalRadius, anchorY + verticalRadius, anchorZ + horizontalRadius
        );
    }

    /**
     * Returns true if the given packed {@code BlockPos.asLong()} falls within this region (inclusive).
     * Pure integer arithmetic — no Minecraft types imported.
     */
    public boolean contains(long packedPos) {
        int x = PackedPos.x(packedPos);
        int y = PackedPos.y(packedPos);
        int z = PackedPos.z(packedPos);

        return x >= this.minX && x <= this.maxX
                && y >= this.minY && y <= this.maxY
                && z >= this.minZ && z <= this.maxZ;
    }

}
