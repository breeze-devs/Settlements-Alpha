package dev.breezes.settlements.domain.ai.memory;

import dev.breezes.settlements.domain.time.ClockTicks;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Descriptor for a named agent memory slot.
 * <p>
 * Sealed to exactly two implementations:
 * <ul>
 *   <li>{@link VanillaMemoryType} — wired into the vanilla {@code Brain}; required for memories
 *       that vanilla GateBehavior/MemoryStatus machinery reads (e.g. PLAN_BEHAVIOR_ACTIVE).</li>
 *   <li>{@link DecayingSpatialMemoryType} — backed by {@code SettlementsMemoryStore} with per-entry
 *       TTL decay; payload is pinned to {@code List<GlobalPos>} at the type level, which is what
 *       eliminates the cast that previously existed in VillagerBrain.</li>
 * </ul>
 * Read/write/clear behavior lives ON the type (not in a dispatcher) so the type parameter {@code T}
 * is structurally carried through to the backing call without any runtime cast.
 * <p>
 * Capping is a decaying-only concept. Vanilla-backed memories are uncapped; the decaying type
 * carries a {@code maxEntries} site cap enforced in two complementary places: a bounded
 * nearest-K selection at the index query, and stalest-first eviction inside the store.
 */
public sealed interface MemoryType<T> permits MemoryType.VanillaMemoryType, MemoryType.DecayingSpatialMemoryType {

    String identifier();

    /**
     * Reads this memory from the given access. Returns empty when no value is present
     * (or when on the client for decaying types, since GlobalPos requires a dimension key).
     */
    Optional<T> read(@Nonnull MemoryAccess access);

    /**
     * Writes a value to this memory. For decaying memories, calling this throws
     * {@link UnsupportedOperationException} — they are written via the observation update path.
     */
    void write(@Nonnull MemoryAccess access, @Nonnull T value);

    /**
     * Writes a value with an expiration. For decaying memories, calling this throws
     * {@link UnsupportedOperationException}.
     */
    void write(@Nonnull MemoryAccess access, @Nonnull T value, long expirationTicks);

    /**
     * Clears this memory from the given access.
     */
    void clear(@Nonnull MemoryAccess access);

    /**
     * Creates an uncapped vanilla-backed memory for scalar/entity/flag/list memories wired into
     * the vanilla {@code Brain}. Block-resource site lists use {@link #decaying} instead.
     */
    static <T> VanillaMemoryType<T> vanillaBacked(@Nonnull String identifier,
                                                  @Nonnull Supplier<MemoryModuleType<T>> moduleTypeSupplier) {
        return new VanillaMemoryType<>(identifier, moduleTypeSupplier);
    }

    /**
     * Creates a decaying memory backed by {@code SettlementsMemoryStore}.
     * No vanilla MemoryModuleType is required; the vanilla Brain is not consulted.
     * Payload is pinned to {@code List<GlobalPos>} at the type level.
     *
     * @param retention  how long an entry is remembered after its last observed tick; measured
     *                   against the monotonic world game-time, hence {@link ClockTicks}
     * @param maxEntries maximum entries before stalest-first eviction
     */
    static DecayingSpatialMemoryType decaying(@Nonnull String identifier,
                                              @Nonnull ClockTicks retention,
                                              int maxEntries) {
        if (maxEntries < 1) {
            throw new IllegalArgumentException("maxEntries must be at least 1, got " + maxEntries + " for '" + identifier + "'");
        }
        return new DecayingSpatialMemoryType(identifier, retention.getTicks(), maxEntries);
    }

    /**
     * Vanilla-backed memory: all I/O routes through {@code MemoryAccess.vanillaGet/Set/Erase}.
     * {@code T} is the exact vanilla payload type — no cast is required at the call site because
     * the {@code MemoryModuleType<T>} and the accessor generic parameter line up structurally.
     */
    record VanillaMemoryType<T>(@Nonnull String identifier,
                                @Nonnull Supplier<MemoryModuleType<T>> moduleTypeSupplier) implements MemoryType<T> {

        /**
         * Returns the underlying vanilla MemoryModuleType.
         * Only accessible on VanillaMemoryType — the concrete type exposes this,
         * the sealed supertype does not, so callers that need it must hold a VanillaMemoryType
         * reference (registry fields are declared as the concrete type for exactly this reason).
         */
        public MemoryModuleType<T> getModuleType() {
            return this.moduleTypeSupplier.get();
        }

        @Override
        public Optional<T> read(@Nonnull MemoryAccess access) {
            return access.vanillaGet(this.moduleTypeSupplier.get());
        }

        @Override
        public void write(@Nonnull MemoryAccess access, @Nonnull T value) {
            access.vanillaSet(this.moduleTypeSupplier.get(), value);
        }

        @Override
        public void write(@Nonnull MemoryAccess access, @Nonnull T value, long expirationTicks) {
            access.vanillaSet(this.moduleTypeSupplier.get(), value, expirationTicks);
        }

        @Override
        public void clear(@Nonnull MemoryAccess access) {
            access.vanillaErase(this.moduleTypeSupplier.get());
        }

    }

    /**
     * Decaying spatial memory: payload is pinned to {@code List<GlobalPos>} at the type level.
     * This is the structural guarantee that eliminates the footgun cast: the impl knows its own T,
     * so the store methods accept it without any runtime inspection.
     * <p>
     * Writing a value directly is not supported because the decaying store uses an observation-update
     * (upsert + confirmed-absence) protocol rather than wholesale overwrite. Any attempt to call
     * {@link #write} here is a programming error — the caller must use {@link IBrain#updateObservation}.
     */
    record DecayingSpatialMemoryType(@Nonnull String identifier,
                                     long retentionTicks,
                                     int maxEntries) implements MemoryType<List<GlobalPos>> {

        @Override
        public Optional<List<GlobalPos>> read(@Nonnull MemoryAccess access) {
            // Decaying memories need the server dimension key to wrap BlockPos in GlobalPos.
            // On the client (or when the level isn't a ServerLevel) this is null, so return empty.
            @Nullable
            ResourceKey<Level> dimension = access.serverDimensionOrNull();
            if (dimension == null) {
                return Optional.empty();
            }

            return access.decayingStore().getSpatialMemory(this, dimension, access.nowTick());
        }

        @Override
        public void write(@Nonnull MemoryAccess access, @Nonnull List<GlobalPos> value) {
            // Decaying memories are written via the observation update path, not wholesale overwrite.
            // Reaching here from a vanilla-style setMemory call is a caller bug.
            throw new UnsupportedOperationException(
                    "Decaying memory '" + this.identifier + "' must be written via IBrain.updateObservation, not setMemory");
        }

        @Override
        public void write(@Nonnull MemoryAccess access, @Nonnull List<GlobalPos> value, long expirationTicks) {
            throw new UnsupportedOperationException(
                    "Decaying memory '" + this.identifier + "' does not support timed setMemory");
        }

        @Override
        public void clear(@Nonnull MemoryAccess access) {
            access.decayingStore().clearSpatialMemory(this);
        }

    }

}
