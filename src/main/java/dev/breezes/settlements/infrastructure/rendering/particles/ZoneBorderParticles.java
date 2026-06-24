package dev.breezes.settlements.infrastructure.rendering.particles;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3f;

import javax.annotation.Nonnull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ZoneBorderParticles {

    /**
     * Space between consecutive particles along each edge.
     * A fixed interval produces equal visual density regardless of edge length,
     * avoiding the sparse corners that arise from a fixed particle count per edge.
     */
    private static final double PARTICLE_INTERVAL_BLOCKS = 0.5D;

    private static final Vector3f COLOR_ZONE_GREEN = new Vector3f(0.15F, 0.95F, 0.25F);
    private static final Vector3f COLOR_ZONE_RED = new Vector3f(0.95F, 0.15F, 0.15F);

    public static void spawnCultivationZone(@Nonnull ServerLevel level,
                                            @Nonnull BlockPos totemPos,
                                            int halfExtentX,
                                            int halfExtentZ,
                                            boolean valid) {
        // Particles sit at the farmland surface (totem Y + small lift), not at water Y (totem Y − 1).
        // The farmland is one block above the water source so the outline reads as "this is your field."
        double baseY = totemPos.getY() + 0.3D;

        // Four corner coordinates of the zone perimeter in XZ
        double minX = totemPos.getX() - halfExtentX;
        double maxX = totemPos.getX() + halfExtentX + 1.0D;
        double minZ = totemPos.getZ() - halfExtentZ;
        double maxZ = totemPos.getZ() + halfExtentZ + 1.0D;

        Vector3f color = valid ? COLOR_ZONE_GREEN : COLOR_ZONE_RED;
        DustParticleOptions dust = new DustParticleOptions(color, 1.1F);

        spawnEdgeParticles(level, dust, minX, baseY, minZ, maxX, baseY, minZ);
        spawnEdgeParticles(level, dust, minX, baseY, maxZ, maxX, baseY, maxZ);
        spawnEdgeParticles(level, dust, minX, baseY, minZ, minX, baseY, maxZ);
        spawnEdgeParticles(level, dust, maxX, baseY, minZ, maxX, baseY, maxZ);
    }

    private static void spawnEdgeParticles(@Nonnull ServerLevel level,
                                           @Nonnull DustParticleOptions dust,
                                           double x1, double y1, double z1,
                                           double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Steps derived from edge length so density stays constant across differently-sized zones
        int steps = Math.max(1, (int) Math.ceil(length / PARTICLE_INTERVAL_BLOCKS));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double x = x1 + dx * t;
            double y = y1 + dy * t;
            double z = z1 + dz * t;
            level.sendParticles(dust, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

}
