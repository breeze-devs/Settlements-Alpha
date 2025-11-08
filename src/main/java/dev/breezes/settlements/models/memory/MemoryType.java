package dev.breezes.settlements.models.memory;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

@Builder
@Getter
public class MemoryType<T> {

    private final String identifier;

    private final Class<T> memoryClass;

    @Setter
    private MemoryModuleType<T> moduleType;

}
