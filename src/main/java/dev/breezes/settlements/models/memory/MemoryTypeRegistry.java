package dev.breezes.settlements.models.memory;

import dev.breezes.settlements.entities.villager.ISettlementsVillager;
import dev.breezes.settlements.models.memory.temp.TMemoryFarmland;
import dev.breezes.settlements.util.ResourceLocationUtil;
import lombok.CustomLog;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

@CustomLog
public class MemoryTypeRegistry {

    public static final MemoryType<GlobalPos> NEAREST_HARVESTABLE_SUGARCANE = register(MemoryType.<GlobalPos>builder()
            .identifier("nearest_harvestable_sugarcane")
            .build());

    public static final MemoryType<ISettlementsVillager> INTERACT_TARGET = register(MemoryType.<ISettlementsVillager>builder()
            .identifier("interact_target")
            .build());

    public static final MemoryType<List<TMemoryFarmland>> OWNED_FARMLAND = register(MemoryType.<List<TMemoryFarmland>>builder()
            .identifier("owned_farmland")
            .build());

    private static <T> MemoryType<T> register(@Nonnull MemoryType<T> memory) {
        ResourceLocation location = ResourceLocationUtil.mod(memory.getIdentifier());
        log.debug("Registering memory module {}", location.toString());

        MemoryModuleType<T> module = Registry.register(BuiltInRegistries.MEMORY_MODULE_TYPE,
                ResourceLocationUtil.mod(memory.getIdentifier()),
                new MemoryModuleType<>(Optional.empty()));
        memory.setModuleType(module);
        return memory;
    }

}
