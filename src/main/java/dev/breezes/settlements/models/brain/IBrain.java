package dev.breezes.settlements.models.brain;

import dev.breezes.settlements.models.memory.MemoryType;
import dev.breezes.settlements.models.memory.entry.IMemoryEntry;
import dev.breezes.settlements.util.Ticks;

import javax.annotation.Nonnull;
import java.util.Optional;

public interface IBrain {

    /**
     * Called every delta ticks to update the brain, this should be called frequently
     */
    void tick(int delta);

    /*
     * Memory management methods
     */
    <T> Optional<IMemoryEntry<T>> getMemory(@Nonnull MemoryType<T> type);

    <T> void setMemory(@Nonnull MemoryType<T> type, @Nonnull IMemoryEntry<T> memory);

    // TODO: evaluate how we should set expiration
    <T> void setMemory(@Nonnull MemoryType<T> type, @Nonnull IMemoryEntry<T> memory, @Nonnull Ticks expiration);

    void clearMemory(@Nonnull MemoryType<?> type);

    /**
     * Loops through all memories and removes any expired ones
     */
    void checkAndExpireMemories();

}
