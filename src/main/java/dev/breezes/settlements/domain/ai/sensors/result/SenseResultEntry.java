package dev.breezes.settlements.domain.ai.sensors.result;

import dev.breezes.settlements.domain.ai.memory.MemoryType;
import dev.breezes.settlements.domain.ai.memory.entry.IMemoryEntry;
import lombok.Builder;
import lombok.Getter;

import java.util.Optional;

@Builder
@Getter
public class SenseResultEntry<T> implements ISenseResultEntry<T> {

    private final MemoryType<T> memoryType;
    private final IMemoryEntry<T> memoryEntry;

    @Override
    public Optional<IMemoryEntry<T>> getMemoryEntry() {
        return Optional.of(memoryEntry);
    }

}
