package dev.breezes.settlements.particles;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nonnull;

// TODO: we should turn this into a registry, not a utility class
public class ParticleRegistry {

    public static void repairIronGolem(@Nonnull ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.WAX_OFF, x, y, z, 15, 0.5, 0.8, 0.5, 0.1);
    }

}
