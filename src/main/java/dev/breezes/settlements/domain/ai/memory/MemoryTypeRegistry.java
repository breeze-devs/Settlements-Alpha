package dev.breezes.settlements.domain.ai.memory;

import dev.breezes.settlements.bootstrap.registry.memory.MemoryModuleTypeRegistry;
import dev.breezes.settlements.domain.ai.perception.PerceivedEntities;
import dev.breezes.settlements.domain.entities.ISettlementsVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.core.GlobalPos;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class MemoryTypeRegistry {

    public static final MemoryType<ISettlementsVillager> INTERACT_TARGET = MemoryType.<ISettlementsVillager>builder()
            .identifier("interact_target")
            .moduleTypeSupplier(MemoryModuleTypeRegistry.INTERACT_TARGET)
            .build();

    public static final MemoryType<Boolean> PLAN_BEHAVIOR_ACTIVE = MemoryType.<Boolean>builder()
            .identifier("plan_behavior_active")
            .moduleTypeSupplier(MemoryModuleTypeRegistry.PLAN_BEHAVIOR_ACTIVE)
            .build();

    public static final MemoryType<Set<GlobalPos>> FENCE_GATES_TO_CLOSE = MemoryType.<Set<GlobalPos>>builder()
            .identifier("fence_gates_to_close")
            .moduleTypeSupplier(MemoryModuleTypeRegistry.FENCE_GATES_TO_CLOSE)
            .build();

    public static final MemoryType<List<UUID>> OWNED_WOLVES = MemoryType.<List<UUID>>builder()
            .identifier("owned_wolves")
            .moduleTypeSupplier(MemoryModuleTypeRegistry.OWNED_WOLVES)
            .build();

    public static final MemoryType<List<GlobalPos>> VILLAGE_CHESTS = MemoryType.<List<GlobalPos>>builder()
            .identifier("village_chests")
            .moduleTypeSupplier(MemoryModuleTypeRegistry.VILLAGE_CHESTS)
            .build();

    public static final MemoryType<List<GlobalPos>> RIPE_PUMPKIN_SITES = MemoryType.<List<GlobalPos>>builder()
            .identifier("ripe_pumpkin_sites")
            .moduleTypeSupplier(MemoryModuleTypeRegistry.RIPE_PUMPKIN_SITES)
            .build();

    public static final MemoryType<List<GlobalPos>> RIPE_MELON_SITES = MemoryType.<List<GlobalPos>>builder()
            .identifier("ripe_melon_sites")
            .moduleTypeSupplier(MemoryModuleTypeRegistry.RIPE_MELON_SITES)
            .build();

    public static final MemoryType<List<GlobalPos>> RIPE_SWEET_BERRY_BUSH_SITES = MemoryType.<List<GlobalPos>>builder()
            .identifier("ripe_sweet_berry_bush_sites")
            .moduleTypeSupplier(MemoryModuleTypeRegistry.RIPE_SWEET_BERRY_BUSH_SITES)
            .build();

    public static final MemoryType<List<GlobalPos>> RIPE_CROP_SITES = MemoryType.<List<GlobalPos>>builder()
            .identifier("ripe_crop_sites")
            .moduleTypeSupplier(MemoryModuleTypeRegistry.RIPE_CROP_SITES)
            .build();

    public static final MemoryType<List<GlobalPos>> NETHER_WART_FARM_SITES = MemoryType.<List<GlobalPos>>builder()
            .identifier("nether_wart_farm_sites")
            .moduleTypeSupplier(MemoryModuleTypeRegistry.NETHER_WART_FARM_SITES)
            .build();

    public static final MemoryType<List<GlobalPos>> HARVESTABLE_SUGARCANE_SITES = MemoryType.<List<GlobalPos>>builder()
            .identifier("harvestable_sugarcane_sites")
            .moduleTypeSupplier(MemoryModuleTypeRegistry.HARVESTABLE_SUGARCANE_SITES)
            .build();

    public static final MemoryType<List<GlobalPos>> FULL_HIVE_SITES = MemoryType.<List<GlobalPos>>builder()
            .identifier("full_hive_sites")
            .moduleTypeSupplier(MemoryModuleTypeRegistry.FULL_HIVE_SITES)
            .build();

    public static final MemoryType<List<GlobalPos>> ORE_SITES = MemoryType.<List<GlobalPos>>builder()
            .identifier("ore_sites")
            .moduleTypeSupplier(MemoryModuleTypeRegistry.ORE_SITES)
            .build();

    public static final MemoryType<List<GlobalPos>> GRAVEL_SITES = MemoryType.<List<GlobalPos>>builder()
            .identifier("gravel_sites")
            .moduleTypeSupplier(MemoryModuleTypeRegistry.GRAVEL_SITES)
            .build();

    public static final MemoryType<List<GlobalPos>> SAND_SITES = MemoryType.<List<GlobalPos>>builder()
            .identifier("sand_sites")
            .moduleTypeSupplier(MemoryModuleTypeRegistry.SAND_SITES)
            .build();

    public static final MemoryType<List<GlobalPos>> CULTIVATION_TOTEM_SITES = MemoryType.<List<GlobalPos>>builder()
            .identifier("cultivation_totem_sites")
            .moduleTypeSupplier(MemoryModuleTypeRegistry.CULTIVATION_TOTEM_SITES)
            .build();

    public static final MemoryType<List<UUID>> WILLING_COURTSHIP_PARTNERS = MemoryType.<List<UUID>>builder()
            .identifier("willing_courtship_partners")
            .moduleTypeSupplier(MemoryModuleTypeRegistry.WILLING_COURTSHIP_PARTNERS)
            .build();

    public static final MemoryType<PerceivedEntities> NEARBY_SENSED_ENTITIES = MemoryType.<PerceivedEntities>builder()
            .identifier("nearby_sensed_entities")
            .moduleTypeSupplier(MemoryModuleTypeRegistry.NEARBY_SENSED_ENTITIES)
            .build();

}
