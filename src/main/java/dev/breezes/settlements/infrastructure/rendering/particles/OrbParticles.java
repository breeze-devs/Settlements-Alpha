package dev.breezes.settlements.infrastructure.rendering.particles;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

/**
 * Stateless helper that spawns the two orb bursts for the Totem of Cultivation ambient aura.
 * <p>
 * Mirrors the shape of {@link ZoneBorderParticles} — a final utility class with no state,
 * called from the block entity's client ticker. Keeping spawn logic here rather than inline in
 * the BE keeps the ticker method lean and the spawn math independently readable.
 * <p>
 * All positions are in world space. The caller is responsible for rate-gating.
 */
public final class OrbParticles {

    /**
     * Upward velocity seed for RISE orbs — gentle enough that the lift feels floaty
     */
    private static final double RISE_BASE_VELOCITY_Y = 0.18;
    private static final double RISE_HORIZONTAL_SPREAD = 0.55;

    /**
     * Downward velocity seed for SCATTER orbs; lateral spread is controlled by the half-extents
     */
    private static final double SCATTER_BASE_VELOCITY_Y = -0.03;
    private static final double SCATTER_HORIZONTAL_SPEED = 0.04;

    private OrbParticles() {
    }

    /**
     * Spawns a single white RISE orb near the lily-pad base, drifting upward.
     *
     * @param level    client level
     * @param totemPos position of the totem block (lily-pad surface)
     * @param random   random source for jitter
     */
    public static void spawnRise(@Nonnull Level level,
                                 @Nonnull BlockPos totemPos,
                                 @Nonnull RandomSource random) {
        // Spawn just above the lily-pad surface; small X/Z jitter keeps orbs from stacking
        double x = totemPos.getX() + 0.5 + (random.nextDouble() - 0.5) * RISE_HORIZONTAL_SPREAD;
        double y = totemPos.getY() + 0.1 + random.nextDouble() * 0.1;
        double z = totemPos.getZ() + 0.5 + (random.nextDouble() - 0.5) * RISE_HORIZONTAL_SPREAD;

        double dx = (random.nextDouble() - 0.5) * 0.01;
        double dy = RISE_BASE_VELOCITY_Y + random.nextDouble() * 0.01;
        double dz = (random.nextDouble() - 0.5) * 0.01;

        level.addParticle(OrbParticleOptions.RISE_WHITE, x, y, z, dx, dy, dz);
    }

    /**
     * Spawns a single lime-green SCATTER orb near the floating totem mesh, drifting downward
     * and outward into the farm zone footprint.
     *
     * @param level       client level
     * @param totemPos    position of the totem block (lily-pad surface)
     * @param floatHeight how many blocks above the totem block the mesh floats (shared constant)
     * @param halfExtentX half-width of the farm zone on the X axis
     * @param halfExtentZ half-width of the farm zone on the Z axis
     * @param random      random source for jitter
     */
    public static void spawnScatter(@Nonnull Level level,
                                    @Nonnull BlockPos totemPos,
                                    float floatHeight,
                                    int halfExtentX,
                                    int halfExtentZ,
                                    @Nonnull RandomSource random) {
        // Spawn near the floating mesh center with small position jitter
        double x = totemPos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3;
        double y = totemPos.getY() + floatHeight + (random.nextDouble() - 0.5) * 0.2;
        double z = totemPos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3;

        // Lateral velocity is biased outward toward a random point within the zone footprint,
        // so orbs scatter into the field without targeting individual cells (no per-tick stream).
        double targetOffsetX = (random.nextDouble() * 2.0 - 1.0) * halfExtentX;
        double targetOffsetZ = (random.nextDouble() * 2.0 - 1.0) * halfExtentZ;

        double dx = targetOffsetX * SCATTER_HORIZONTAL_SPEED;
        double dy = SCATTER_BASE_VELOCITY_Y - random.nextDouble() * 0.01;
        double dz = targetOffsetZ * SCATTER_HORIZONTAL_SPEED;

        level.addParticle(OrbParticleOptions.SCATTER_LIME, x, y, z, dx, dy, dz);
    }

}
