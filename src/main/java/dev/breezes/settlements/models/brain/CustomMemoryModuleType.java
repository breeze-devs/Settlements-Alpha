package dev.breezes.settlements.models.brain;

import com.mojang.serialization.Codec;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import java.util.Optional;
import java.util.Set;

public class CustomMemoryModuleType<U> extends MemoryModuleType<U> {
    public static final MemoryModuleType<Set<GlobalPos>> FENCE_GATES_TO_CLOSE;

    //copied from parent class
    private static <U> MemoryModuleType<U> register(String identifier, Codec<U> codec) {
        return Registry.register(BuiltInRegistries.MEMORY_MODULE_TYPE, ResourceLocation.withDefaultNamespace(identifier), new MemoryModuleType<>(Optional.of(codec)));
    }

    private static <U> MemoryModuleType<U> register(String identifier) {
        return Registry.register(BuiltInRegistries.MEMORY_MODULE_TYPE, ResourceLocation.withDefaultNamespace(identifier), new MemoryModuleType<>(Optional.empty()));
    }
    public CustomMemoryModuleType(Optional<Codec<U>> uCodec) {
        super(uCodec);
    }

    static {
        FENCE_GATES_TO_CLOSE = register("fence_gates_to_close");
    }
}
