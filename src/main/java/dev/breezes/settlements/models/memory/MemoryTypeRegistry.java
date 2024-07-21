package dev.breezes.settlements.models.memory;

import lombok.Getter;
import net.minecraft.core.GlobalPos;

@Getter
public class MemoryTypeRegistry {

    public static final MemoryType<GlobalPos> NEAREST_HARVESTABLE_SUGARCANE = MemoryType.<GlobalPos>builder()
            .identifier("nearest_harvestable_sugarcane")
            .serializer(null) // TODO: Implement serializer
            .build();

}
