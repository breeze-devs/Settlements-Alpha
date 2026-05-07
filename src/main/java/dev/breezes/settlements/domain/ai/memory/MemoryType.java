package dev.breezes.settlements.domain.ai.memory;

import lombok.Builder;
import lombok.Getter;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import java.util.function.Supplier;

@Builder
@Getter
public class MemoryType<T> {

    private final String identifier;

    private final Class<T> memoryClass;

    private final Supplier<MemoryModuleType<T>> moduleTypeSupplier;

    public MemoryModuleType<T> getModuleType() {
        return this.moduleTypeSupplier.get();
    }

}
