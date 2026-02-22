package dev.breezes.settlements.domain.ai.memory.entry;

import dev.breezes.settlements.domain.ai.brain.IBrain;
import dev.breezes.settlements.domain.ai.memory.MemoryType;

import javax.annotation.Nonnull;

public interface IMemoryEntry<T> {

    MemoryType<T> getMemoryType();

    T getMemoryValue();

    /**
     * Priority of the memory, higher priority memories (lower int value) will be kept over lower priority ones (higher int value)
     */
    int getMemoryPriority();

    void saveToBrain(@Nonnull IBrain brain);

}
