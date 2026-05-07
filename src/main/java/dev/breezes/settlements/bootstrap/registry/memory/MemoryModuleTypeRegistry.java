package dev.breezes.settlements.bootstrap.registry.memory;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.domain.entities.ISettlementsVillager;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public final class MemoryModuleTypeRegistry {

    public static final DeferredRegister<MemoryModuleType<?>> REGISTRY =
            DeferredRegister.create(BuiltInRegistries.MEMORY_MODULE_TYPE, SettlementsMod.MOD_ID);

    public static final Supplier<MemoryModuleType<GlobalPos>> NEAREST_HARVESTABLE_SUGARCANE = REGISTRY.register(
            "nearest_harvestable_sugarcane",
            () -> new MemoryModuleType<>(Optional.empty()));

    public static final Supplier<MemoryModuleType<ISettlementsVillager>> INTERACT_TARGET = REGISTRY.register(
            "interact_target",
            () -> new MemoryModuleType<>(Optional.empty()));

    public static final Supplier<MemoryModuleType<Boolean>> PLAN_BEHAVIOR_ACTIVE = REGISTRY.register(
            "plan_behavior_active",
            () -> new MemoryModuleType<>(Optional.empty()));

    public static final Supplier<MemoryModuleType<Set<GlobalPos>>> FENCE_GATES_TO_CLOSE = REGISTRY.register(
            "fence_gates_to_close",
            () -> new MemoryModuleType<>(Optional.empty()));

    private MemoryModuleTypeRegistry() {
    }

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }

}
