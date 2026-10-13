package dev.breezes.settlements.infrastructure.rendering.particles;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

/**
 * Stateless helper that spawns the Cultivation Lily's orb effects. The ambient aura and the bloom
 * ring spawn on the client; the growth trail is sent from the server.
 * <p>
 * All positions are in world space. The caller is responsible for rate-gating.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
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

    /**
     * Orb count and radius for the bloom's particle ring — enough orbs for the circle to read as a
     * ring rather than a scatter of individual points, at a radius that clears the totem mesh itself.
     */
    private static final int BLOOM_RING_ORB_COUNT = 12;
    private static final double BLOOM_RING_RADIUS = 0.8;
    private static final double BLOOM_RING_OUTWARD_SPEED = 0.02;
    private static final double BLOOM_RING_RISE_VELOCITY_Y = 0.02;

    /**
     * Growth-trail orbs per block of distance from the floating mesh to the crop, so long and short
     * trails read with the same density.
     */
    private static final double GROWTH_TRAIL_ORBS_PER_BLOCK = 1.0;
    /**
     * Maximum horizontal offset of each growth-trail orb's resting point, so the trail reads as a
     * loose strand rather than a ruled line.
     */
    private static final double GROWTH_TRAIL_JITTER = 0.15;
    /**
     * Height above the crop block's base where the growth trail ends, low enough to land on a seedling.
     */
    private static final double GROWTH_TRAIL_END_HEIGHT = 0.3;

    /**
     * Spawns a single white RISE orb near the lily-pad base, drifting upward.
     *
     * @param level   client level
     * @param lilyPos position of the lily block (lily-pad surface)
     * @param random  random source for jitter
     */
    public static void spawnRise(@Nonnull Level level,
                                 @Nonnull BlockPos lilyPos,
                                 @Nonnull RandomSource random) {
        // Spawn just above the lily-pad surface; small X/Z jitter keeps orbs from stacking
        double x = lilyPos.getX() + 0.5 + (random.nextDouble() - 0.5) * RISE_HORIZONTAL_SPREAD;
        double y = lilyPos.getY() + 0.1 + random.nextDouble() * 0.1;
        double z = lilyPos.getZ() + 0.5 + (random.nextDouble() - 0.5) * RISE_HORIZONTAL_SPREAD;

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
     * @param lilyPos     position of the lily block (lily-pad surface)
     * @param floatHeight how many blocks above the lily block the mesh floats (shared constant)
     * @param halfExtentX half-width of the farm zone on the X axis
     * @param halfExtentZ half-width of the farm zone on the Z axis
     * @param random      random source for jitter
     */
    public static void spawnScatter(@Nonnull Level level,
                                    @Nonnull BlockPos lilyPos,
                                    float floatHeight,
                                    int halfExtentX,
                                    int halfExtentZ,
                                    @Nonnull RandomSource random) {
        // Spawn near the floating mesh center with small position jitter
        double x = lilyPos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3;
        double y = lilyPos.getY() + floatHeight + (random.nextDouble() - 0.5) * 0.2;
        double z = lilyPos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3;

        // Lateral velocity is biased outward toward a random point within the zone footprint,
        // so orbs scatter into the field without targeting individual cells (no per-tick stream).
        double targetOffsetX = (random.nextDouble() * 2.0 - 1.0) * halfExtentX;
        double targetOffsetZ = (random.nextDouble() * 2.0 - 1.0) * halfExtentZ;

        double dx = targetOffsetX * SCATTER_HORIZONTAL_SPEED;
        double dy = SCATTER_BASE_VELOCITY_Y - random.nextDouble() * 0.01;
        double dz = targetOffsetZ * SCATTER_HORIZONTAL_SPEED;

        level.addParticle(OrbParticleOptions.SCATTER_LIME, x, y, z, dx, dy, dz);
    }

    /**
     * Spawns a ring of lime-green orbs around the floating-mesh height, marking the moment a lily
     * blooms. A single burst arranged in a circle, rather than {@link #spawnScatter}'s per-tick
     * trickle, is what reads as one event instead of the ambient aura's continuous drift.
     */
    public static void spawnBloomRing(@Nonnull Level level,
                                      @Nonnull BlockPos lilyPos,
                                      float floatHeight) {
        double centerX = lilyPos.getX() + 0.5;
        double centerY = lilyPos.getY() + floatHeight;
        double centerZ = lilyPos.getZ() + 0.5;

        for (int i = 0; i < BLOOM_RING_ORB_COUNT; i++) {
            double angle = (Math.PI * 2 * i) / BLOOM_RING_ORB_COUNT;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            double x = centerX + cos * BLOOM_RING_RADIUS;
            double z = centerZ + sin * BLOOM_RING_RADIUS;
            double dx = cos * BLOOM_RING_OUTWARD_SPEED;
            double dz = sin * BLOOM_RING_OUTWARD_SPEED;

            level.addParticle(OrbParticleOptions.SCATTER_LIME, x, centerY, z, dx, BLOOM_RING_RISE_VELOCITY_Y, dz);
        }
    }

    /**
     * Sends a strand of lime-green orbs from the floating mesh to a crop the lily just grew, to every
     * player in vanilla particle range. All orbs launch together from the mesh and come to rest at
     * evenly spaced points along the way, so the strand extends from the mesh and ends on the crop.
     */
    public static void spawnGrowthTrail(@Nonnull ServerLevel level,
                                        @Nonnull BlockPos lilyPos,
                                        float floatHeight,
                                        @Nonnull BlockPos cropPos,
                                        @Nonnull RandomSource random) {
        double startX = lilyPos.getX() + 0.5;
        double startY = lilyPos.getY() + floatHeight;
        double startZ = lilyPos.getZ() + 0.5;

        double offsetX = cropPos.getX() + 0.5 - startX;
        double offsetY = cropPos.getY() + GROWTH_TRAIL_END_HEIGHT - startY;
        double offsetZ = cropPos.getZ() + 0.5 - startZ;

        double distance = Math.sqrt(offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ);
        int orbCount = Math.max(1, (int) Math.ceil(distance * GROWTH_TRAIL_ORBS_PER_BLOCK));

        for (int i = 1; i <= orbCount; i++) {
            double fraction = (double) i / orbCount;
            double restX = offsetX * fraction + (random.nextDouble() * 2.0 - 1.0) * GROWTH_TRAIL_JITTER;
            double restY = offsetY * fraction;
            double restZ = offsetZ * fraction + (random.nextDouble() * 2.0 - 1.0) * GROWTH_TRAIL_JITTER;

            // A zero count makes the client spawn exactly one orb and read the three offsets as its velocity
            level.sendParticles(OrbParticleOptions.DRIFT_LIME, startX, startY, startZ, 0,
                    OrbParticleOptions.driftVelocityToRestAt(restX),
                    OrbParticleOptions.driftVelocityToRestAt(restY),
                    OrbParticleOptions.driftVelocityToRestAt(restZ),
                    1.0);
        }
    }

}
