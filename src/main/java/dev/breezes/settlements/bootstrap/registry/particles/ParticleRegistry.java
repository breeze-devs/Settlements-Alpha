package dev.breezes.settlements.bootstrap.registry.particles;

import dev.breezes.settlements.domain.world.location.Location;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
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

    public static void digBlockCrack(@Nonnull Location location, @Nonnull BlockState state) {
        blockCrack(location, state, 6, 0.35, 0.15, 0.35, 0.0D);
    }

    public static void harvestBlock(@Nonnull Location location, @Nonnull BlockState state) {
        blockCrack(location, state, 24, 0.35, 0.35, 0.35, 0.0D);
    }

    public static <T extends ParticleOptions> void displayCircle(@Nonnull T particleType,
                                                                 @Nonnull Location center,
                                                                 double radius,
                                                                 int sampleCount) {
        if (radius < 0 || sampleCount <= 0) {
            return;
        }

        for (int i = 0; i < sampleCount; i++) {
            double angle = (Math.PI * 2 * i) / sampleCount;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);

            Location.of(x, center.getY(), z, center.getLevel().orElse(null))
                    .displayParticles(particleType, 1, 0, 0, 0, 0);
        }
    }

}
