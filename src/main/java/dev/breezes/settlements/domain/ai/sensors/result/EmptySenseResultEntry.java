package dev.breezes.settlements.domain.ai.sensors.result;

import dev.breezes.settlements.domain.ai.memory.MemoryType;
import dev.breezes.settlements.domain.ai.memory.entry.IMemoryEntry;
import lombok.Builder;
import lombok.Getter;

import java.util.Optional;

@Builder
@Getter
public class EmptySenseResultEntry<T> implements ISenseResultEntry<T> {

    private final MemoryType<T> memoryType;

    @Override
    public Optional<IMemoryEntry<T>> getMemoryEntry() {
        return Optional.empty();
    }

}
