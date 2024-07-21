package dev.breezes.settlements.models.sensors.result;

import dev.breezes.settlements.models.brain.IBrain;
import dev.breezes.settlements.models.memory.MemoryType;
import dev.breezes.settlements.models.memory.entry.IMemoryEntry;

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
