package dev.breezes.settlements.domain.ai.brain;

import dev.breezes.settlements.domain.ai.memory.MemoryType;
import dev.breezes.settlements.domain.time.ClockTicks;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.Optional;

public interface IBrain {

    /**
     * Wires up server-scoped state (e.g. sensors) for the brain's owner.
     * <p>
     * Called once on the server when the entity spawns or loads, before the first {@link #tick(int)}.
     * The constructor cannot do this: the server graph is unavailable during entity construction and on the client.
     */
    void initialize();

    /**
     * Called every delta ticks to update the brain, this should be called frequently
     */
    void tick(int delta);

    /**
     * Forces all sensors to run their sensing action immediately, bypassing their internal cooldowns.
     * <p>
     * Used by the Investigate behavior on arrival at a tip location so memories reflect the
     * current world state precisely at the moment of investigation rather than waiting for
     * the natural sensor cadence — which might be many ticks away.
     *
     * @param world the server level to sense in
     */
    void forceSensorScan(Level world);

    /*
     * Memory management methods
     */
    <T> Optional<T> getMemory(@Nonnull MemoryType<T> type);

    <T> void setMemory(@Nonnull MemoryType<T> type, @Nonnull T value);

    <T> void setMemory(@Nonnull MemoryType<T> type, @Nonnull T value, @Nonnull ClockTicks expiration);

    void clearMemory(@Nonnull MemoryType<?> type);

}
