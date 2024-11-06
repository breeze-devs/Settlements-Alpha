package dev.breezes.settlements.particles;

import dev.breezes.settlements.models.location.Location;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

// TODO: we should turn this into a registry, not a utility class
public class ParticleRegistry {

    /*
     * Preset particles
     */
    private static void itemBreak(@Nonnull Location location, @Nonnull ItemStack item, int count, double dx, double dy, double dz, double speed) {
        location.displayParticles(new ItemParticleOption(ParticleTypes.ITEM, item), count, dx, dy, dz, speed);
    }

    private static void blockCrack(@Nonnull Location location, @Nonnull BlockState state, int count, double dx, double dy, double dz, double speed) {
        location.displayParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), count, dx, dy, dz, speed);
    }

    /*
     * Custom particles
     */
    public static void repairIronGolem(@Nonnull Location location) {
        location.displayParticles(ParticleTypes.WAX_OFF, 15, 0.5, 0.8, 0.5, 0.1);
    }

    public static void breedHearts(@Nonnull Location location) {
        location.displayParticles(ParticleTypes.HEART, 5, 0.4, 0.4, 0.4, 1);
    }

    public static void breedItemConsume(@Nonnull Location location, @Nonnull ItemStack item) {
        itemBreak(location, item, 20, 0.3, 0.3, 0.3, 0.0D);
    }

    public static void cutBlock(@Nonnull Location location, @Nonnull BlockState state) {
        blockCrack(location, state, 3, 0.4, 0.4, 0.4, 0.0D);
    }

}
