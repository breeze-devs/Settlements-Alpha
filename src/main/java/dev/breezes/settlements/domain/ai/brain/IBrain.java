package dev.breezes.settlements.domain.ai.brain;

import dev.breezes.settlements.domain.ai.memory.MemoryType;
import dev.breezes.settlements.domain.ai.memory.SensedSiteReport;
import dev.breezes.settlements.domain.time.ClockTicks;

import javax.annotation.Nonnull;
import java.util.Optional;

public interface IBrain {

    /**
     * Wires up server-scoped state (e.g. sensors) for the brain's owner.
     * <p>
     * Called once on the server when the entity spawns or loads, before the first {@link #preVanillaAiStep()}.
     * The constructor cannot do this: the server graph is unavailable during entity construction and on the client.
     */
    void initialize();

    /**
     * Makes the decisions every vanilla behavior must read fresh.
     * <p>
     * Called once per server tick, before the entity's vanilla brain ticks and so before
     * {@link #postVanillaAiStep()}; a decision made any later reaches vanilla behaviors a tick late.
     */
    void preVanillaAiStep();

    /**
     * Updates the brain from what the vanilla brain produced this tick.
     * <p>
     * Called once per server tick, after the entity's vanilla brain ticks.
     */
    void postVanillaAiStep();

    /*
     * Memory management methods
     */
    <T> Optional<T> getMemory(@Nonnull MemoryType<T> type);

    <T> void setMemory(@Nonnull MemoryType<T> type, @Nonnull T value);

    <T> void setMemory(@Nonnull MemoryType<T> type, @Nonnull T value, @Nonnull ClockTicks expiration);

    void clearMemory(@Nonnull MemoryType<?> type);

    /**
     * Applies a sensed-site report to a decaying spatial memory store.
     * <p>
     * The parameter is typed as {@link MemoryType.DecayingSpatialMemoryType} (not the sealed
     * supertype) so the compiler enforces at every call site that only decaying memories enter
     * this path — eliminating the runtime {@code isDecaying()} guard that previously existed.
     */
    void updateSites(@Nonnull MemoryType.DecayingSpatialMemoryType type,
                     @Nonnull SensedSiteReport report,
                     long nowTick);

}
