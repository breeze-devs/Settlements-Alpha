package dev.breezes.settlements.infrastructure.minecraft.entities.villager;

import dev.breezes.settlements.bootstrap.registry.particles.ParticleTypeRegistry;
import dev.breezes.settlements.domain.time.ClockTicks;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

/**
 * Client-only emitter for the stream of "Zzz" that rises off a sleeping villager.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SleepingZEmitter {

    private static final int EMIT_CADENCE = ClockTicks.seconds(0.9).getTicksAsInt();
    private static final double HEAD_HEIGHT_OFFSET = 0.5;
    private static final double SPAWN_JITTER = 0.05;

    /**
     * Advances one sleeper's stream by a tick, emitting at most one Z.
     * <p>
     * Must be driven once per game tick rather than from a render call, which runs at render framerate.
     */
    public static void tickSleeper(@Nonnull BaseVillager villager) {
        // Offsetting the cadence by entity id keeps a bunkhouse of sleepers from puffing in unison
        if ((villager.tickCount + villager.getId()) % EMIT_CADENCE != 0) {
            return;
        }

        Level level = villager.level();
        RandomSource random = level.getRandom();
        double x = villager.getX() + (random.nextDouble() - 0.5) * SPAWN_JITTER;
        double y = villager.getY() + HEAD_HEIGHT_OFFSET;
        double z = villager.getZ() + (random.nextDouble() - 0.5) * SPAWN_JITTER;

        level.addParticle(ParticleTypeRegistry.SLEEPING_Z.get(), x, y, z, 0.0, 0.0, 0.0);
    }

}
