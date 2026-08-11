package dev.breezes.settlements.infrastructure.rendering.zone;

import dev.breezes.settlements.domain.farming.CultivationZone;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;

/**
 * Where a cultivation zone's boundary stands in the world, shared by the surface that reveals a placed
 * lily's zone and the one that previews a held lily's. Both must state the same geometry or the preview
 * stops predicting the placement.
 */
@ClientSide
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class CultivationZoneGeometry {

    /**
     * Height above the lily block the boundary stands on: the crop canopy plane, one block above the water.
     */
    private static final double CANOPY_LIFT_BLOCKS = 0.15D;

    /**
     * The zone's true perimeter rectangle, at the height of the boundary line. Flat by construction: the
     * wall's own vertical span is the reach it is drawn with, so carrying a height here as well would give
     * the same quantity two owners. Exact, not inset — what the plot is, rather than where the wall is
     * drawn to keep clear of it.
     */
    static AABB canopyPlane(@Nonnull CultivationZone zone) {
        BlockPos lilyPos = zone.lilyPos();
        double minX = lilyPos.getX() - zone.halfExtentX();
        double maxX = lilyPos.getX() + zone.halfExtentX() + 1.0D;
        double minZ = lilyPos.getZ() - zone.halfExtentZ();
        double maxZ = lilyPos.getZ() + zone.halfExtentZ() + 1.0D;

        return new AABB(minX, canopyY(lilyPos), minZ, maxX, canopyY(lilyPos), maxZ);
    }

    /**
     * The single cell the lily itself occupies, on the same plane as {@link #canopyPlane}.
     */
    static AABB centerCellPlane(@Nonnull BlockPos lilyPos) {
        return new AABB(lilyPos.getX(), canopyY(lilyPos), lilyPos.getZ(),
                lilyPos.getX() + 1.0D, canopyY(lilyPos), lilyPos.getZ() + 1.0D);
    }

    private static double canopyY(@Nonnull BlockPos lilyPos) {
        return lilyPos.getY() + CANOPY_LIFT_BLOCKS;
    }

}
