package dev.breezes.settlements.domain.ai.sensors.result;

import dev.breezes.settlements.domain.ai.brain.IBrain;
import dev.breezes.settlements.domain.ai.memory.MemoryType;
import dev.breezes.settlements.domain.ai.memory.entry.IMemoryEntry;

import javax.annotation.Nonnull;
import java.util.Optional;

public interface ISenseResultEntry<T> {

    MemoryType<T> getMemoryType();

    Optional<IMemoryEntry<T>> getMemoryEntry();

    default boolean hasResult() {
        return this.getMemoryEntry().isPresent();
    }

    default void saveToBrain(@Nonnull IBrain brain) {
        this.getMemoryEntry()
                .ifPresentOrElse(
                        memoryEntry -> memoryEntry.saveToBrain(brain),
                        () -> brain.clearMemory(this.getMemoryType())
                );
    }

}
