package dev.breezes.settlements.models.memory;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class MemoryType<T> {

    private final String identifier;

    private final IMemorySerializer<T> serializer;

}
