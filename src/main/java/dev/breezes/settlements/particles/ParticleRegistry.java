package dev.breezes.settlements.particles;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

// TODO: we should turn this into a registry, not a utility class
public class ParticleRegistry {

    public static void repairIronGolem(@Nonnull ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.WAX_OFF, x, y, z, 15, 0.5, 0.8, 0.5, 0.1);
    }

    public static void breedHearts(@Nonnull ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.HEART, x, y, z, 5, 0.4, 0.4, 0.4, 1);
    }

    public static void itemBreak(@Nonnull ServerLevel level, double x, double y, double z, ItemStack item) {
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, item), x, y, z, 20, 0.3, 0.3, 0.3, 0.0D);
    }

}
