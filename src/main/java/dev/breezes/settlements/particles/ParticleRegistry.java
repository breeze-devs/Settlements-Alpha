package dev.breezes.settlements.particles;

import dev.breezes.settlements.models.location.Location;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

// TODO: we should turn this into a registry, not a utility class
public class ParticleRegistry {

    public static void repairIronGolem(@Nonnull Location location) {
        location.displayParticles(ParticleTypes.WAX_OFF, 15, 0.5, 0.8, 0.5, 0.1);
    }

    public static void breedHearts(@Nonnull Location location) {
        location.displayParticles(ParticleTypes.HEART, 5, 0.4, 0.4, 0.4, 1);
    }

    public static void itemBreak(@Nonnull Location location, ItemStack item) {
        location.displayParticles(new ItemParticleOption(ParticleTypes.ITEM, item), 20, 0.3, 0.3, 0.3, 0.0D);
    }

}
