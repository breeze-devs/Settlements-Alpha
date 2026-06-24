package dev.breezes.settlements.bootstrap.registry.memory;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.domain.ai.perception.PerceivedEntities;
import dev.breezes.settlements.domain.entities.ISettlementsVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class MemoryModuleTypeRegistry {

    public static final DeferredRegister<MemoryModuleType<?>> REGISTRY =
            DeferredRegister.create(BuiltInRegistries.MEMORY_MODULE_TYPE, SettlementsMod.MOD_ID);

    public static final Supplier<MemoryModuleType<ISettlementsVillager>> INTERACT_TARGET = REGISTRY.register(
            "interact_target",
            () -> new MemoryModuleType<>(Optional.empty()));

    public static final Supplier<MemoryModuleType<Boolean>> PLAN_BEHAVIOR_ACTIVE = REGISTRY.register(
            "plan_behavior_active",
            () -> new MemoryModuleType<>(Optional.empty()));

    public static final Supplier<MemoryModuleType<Set<GlobalPos>>> FENCE_GATES_TO_CLOSE = REGISTRY.register(
            "fence_gates_to_close",
            () -> new MemoryModuleType<>(Optional.empty()));

    public static final Supplier<MemoryModuleType<List<UUID>>> OWNED_WOLVES = REGISTRY.register(
            "owned_wolves",
            () -> new MemoryModuleType<>(Optional.empty()));

    public static final Supplier<MemoryModuleType<List<GlobalPos>>> VILLAGE_CHESTS = REGISTRY.register(
            "village_chests",
            () -> new MemoryModuleType<>(Optional.empty()));

    public static final Supplier<MemoryModuleType<List<GlobalPos>>> RIPE_PUMPKIN_SITES = REGISTRY.register(
            "ripe_pumpkin_sites",
            () -> new MemoryModuleType<>(Optional.empty()));

    public static final Supplier<MemoryModuleType<List<GlobalPos>>> RIPE_MELON_SITES = REGISTRY.register(
            "ripe_melon_sites",
            () -> new MemoryModuleType<>(Optional.empty()));

    public static final Supplier<MemoryModuleType<List<GlobalPos>>> RIPE_SWEET_BERRY_BUSH_SITES = REGISTRY.register(
            "ripe_sweet_berry_bush_sites",
            () -> new MemoryModuleType<>(Optional.empty()));

    public static final Supplier<MemoryModuleType<List<GlobalPos>>> RIPE_CROP_SITES = REGISTRY.register(
            "ripe_crop_sites",
            () -> new MemoryModuleType<>(Optional.empty()));

    public static final Supplier<MemoryModuleType<List<GlobalPos>>> NETHER_WART_FARM_SITES = REGISTRY.register(
            "nether_wart_farm_sites",
            () -> new MemoryModuleType<>(Optional.empty()));

    public static final Supplier<MemoryModuleType<List<GlobalPos>>> HARVESTABLE_SUGARCANE_SITES = REGISTRY.register(
            "harvestable_sugarcane_sites",
            () -> new MemoryModuleType<>(Optional.empty()));

    public static final Supplier<MemoryModuleType<List<GlobalPos>>> FULL_HIVE_SITES = REGISTRY.register(
            "full_hive_sites",
            () -> new MemoryModuleType<>(Optional.empty()));

    public static final Supplier<MemoryModuleType<List<GlobalPos>>> ORE_SITES = REGISTRY.register(
            "ore_sites",
            () -> new MemoryModuleType<>(Optional.empty()));

    public static final Supplier<MemoryModuleType<List<GlobalPos>>> GRAVEL_SITES = REGISTRY.register(
            "gravel_sites",
            () -> new MemoryModuleType<>(Optional.empty()));

    public static final Supplier<MemoryModuleType<List<GlobalPos>>> SAND_SITES = REGISTRY.register(
            "sand_sites",
            () -> new MemoryModuleType<>(Optional.empty()));

    public static final Supplier<MemoryModuleType<List<UUID>>> WILLING_COURTSHIP_PARTNERS = REGISTRY.register(
            "willing_courtship_partners",
            () -> new MemoryModuleType<>(Optional.empty()));

    public static final Supplier<MemoryModuleType<PerceivedEntities>> NEARBY_SENSED_ENTITIES = REGISTRY.register(
            "nearby_sensed_entities",
            () -> new MemoryModuleType<>(Optional.empty()));

    public static final Supplier<MemoryModuleType<List<GlobalPos>>> CULTIVATION_TOTEM_SITES = REGISTRY.register(
            "cultivation_totem_sites",
            () -> new MemoryModuleType<>(Optional.empty()));


    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }

}
