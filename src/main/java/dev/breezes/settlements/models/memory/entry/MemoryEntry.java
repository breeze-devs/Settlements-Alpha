package dev.breezes.settlements.models.memory.entry;

import dev.breezes.settlements.models.brain.IBrain;
import dev.breezes.settlements.models.memory.MemoryType;
import lombok.Builder;
import lombok.Getter;

import javax.annotation.Nonnull;

@Getter
public class MemoryEntry<T> implements IMemoryEntry<T> {

    private final MemoryType<T> memoryType;
    private final T memoryValue;
    private final int memoryPriority;

    @Builder
    public MemoryEntry(@Nonnull MemoryType<T> memoryType, @Nonnull T memoryValue, int memoryPriority) {
        this.memoryType = memoryType;
        this.memoryValue = memoryValue;
        this.memoryPriority = memoryPriority;
    }

    public void saveToBrain(@Nonnull IBrain brain) {
        brain.setMemory(this.memoryType, this);
    }

}
