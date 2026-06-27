package dev.breezes.settlements.domain.ai.memory;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Domain-side port through which {@link MemoryType} implementations reach their backing store.
 * <p>
 * Isolates the brain dispatch logic from both the raw vanilla {@code Brain} and the
 * {@link SettlementsMemoryStore} attachment — {@link MemoryType} subtypes call these primitives
 * without ever knowing which concrete brain they are talking to, which is what lets the type
 * system carry the payload type {@code T} all the way through without a cast.
 * <p>
 * Implementations live in the application layer (e.g. as an inner adapter in {@code VillagerBrain}).
 */
public interface MemoryAccess {

    <T> Optional<T> vanillaGet(MemoryModuleType<T> module);

    <T> void vanillaSet(MemoryModuleType<T> module, T value);

    <T> void vanillaSet(MemoryModuleType<T> module, T value, long expirationTicks);

    void vanillaErase(MemoryModuleType<?> module);

    SettlementsMemoryStore decayingStore();

    /**
     * Returns the server dimension key, or {@code null} on the client.
     * Decaying reads return empty when this is null because GlobalPos requires a dimension key.
     */
    @Nullable
    ResourceKey<Level> serverDimensionOrNull();

    long nowTick();

}
