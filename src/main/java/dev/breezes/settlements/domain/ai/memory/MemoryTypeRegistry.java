package dev.breezes.settlements.domain.ai.memory;

import dev.breezes.settlements.bootstrap.registry.memory.MemoryModuleTypeRegistry;
import dev.breezes.settlements.domain.ai.perception.PerceivedEntities;
import dev.breezes.settlements.domain.entities.ISettlementsVillager;
import dev.breezes.settlements.domain.time.ClockTicks;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.core.GlobalPos;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Central registry of all named memory slots.
 * <p>
 * Fields are declared as their concrete subtype ({@link MemoryType.VanillaMemoryType} or
 * {@link MemoryType.DecayingSpatialMemoryType}) so that call sites which need
 * {@code .getModuleType()} can resolve it at compile time without a cast. Passing a concrete
 * field where the sealed supertype {@code MemoryType<T>} is expected continues to work because
 * the subtype implements the interface.
 * <p>
 * Factory selection:
 * <ul>
 *   <li>{@code vanillaBacked} — scalar/entity/flag/list memories wired into the vanilla Brain.</li>
 *   <li>{@code decaying} — block-resource site lists owned by {@code SettlementsMemoryStore} with
 *       per-entry TTL decay and a bounded nearest-K source cap enforced at query time.</li>
 * </ul>
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class MemoryTypeRegistry {

    public static final MemoryType.VanillaMemoryType<ISettlementsVillager> INTERACT_TARGET = MemoryType.vanillaBacked(
            "interact_target", MemoryModuleTypeRegistry.INTERACT_TARGET);

    public static final MemoryType.VanillaMemoryType<Boolean> PLAN_BEHAVIOR_ACTIVE = MemoryType.vanillaBacked(
            "plan_behavior_active", MemoryModuleTypeRegistry.PLAN_BEHAVIOR_ACTIVE);

    public static final MemoryType.VanillaMemoryType<Set<GlobalPos>> FENCE_GATES_TO_CLOSE = MemoryType.vanillaBacked(
            "fence_gates_to_close", MemoryModuleTypeRegistry.FENCE_GATES_TO_CLOSE);

    public static final MemoryType.VanillaMemoryType<List<UUID>> OWNED_WOLVES = MemoryType.vanillaBacked(
            "owned_wolves", MemoryModuleTypeRegistry.OWNED_WOLVES);

    // VILLAGE_CHESTS is written by VillageChestsSensor, not BlockResource, so it does not need a site cap.
    public static final MemoryType.VanillaMemoryType<List<GlobalPos>> VILLAGE_CHESTS = MemoryType.vanillaBacked(
            "village_chests", MemoryModuleTypeRegistry.VILLAGE_CHESTS);

    // CULTIVATION_TOTEM_SITES is written by CultivationTotemSensor, not BlockResource, so it does not need a site cap.
    public static final MemoryType.VanillaMemoryType<List<GlobalPos>> CULTIVATION_TOTEM_SITES = MemoryType.vanillaBacked(
            "cultivation_totem_sites", MemoryModuleTypeRegistry.CULTIVATION_TOTEM_SITES);

    public static final MemoryType.DecayingSpatialMemoryType RIPE_MELON_SITES = MemoryType.decaying(
            "ripe_melon_sites", ClockTicks.minutes(40), 32);

    public static final MemoryType.DecayingSpatialMemoryType RIPE_PUMPKIN_SITES = MemoryType.decaying(
            "ripe_pumpkin_sites", ClockTicks.minutes(40), 32);

    public static final MemoryType.DecayingSpatialMemoryType RIPE_SWEET_BERRY_BUSH_SITES = MemoryType.decaying(
            "ripe_sweet_berry_bush_sites", ClockTicks.minutes(40), 32);

    public static final MemoryType.DecayingSpatialMemoryType RIPE_CROP_SITES = MemoryType.decaying(
            "ripe_crop_sites", ClockTicks.minutes(40), 32);

    public static final MemoryType.DecayingSpatialMemoryType NETHER_WART_FARM_SITES = MemoryType.decaying(
            "nether_wart_farm_sites", ClockTicks.minutes(40), 32);

    public static final MemoryType.DecayingSpatialMemoryType HARVESTABLE_SUGARCANE_SITES = MemoryType.decaying(
            "harvestable_sugarcane_sites", ClockTicks.minutes(40), 32);

    public static final MemoryType.DecayingSpatialMemoryType FULL_HIVE_SITES = MemoryType.decaying(
            "full_hive_sites", ClockTicks.minutes(30), 32);

    // Ore sites are remembered longer because mining targets are visited infrequently.
    public static final MemoryType.DecayingSpatialMemoryType ORE_SITES = MemoryType.decaying(
            "ore_sites", ClockTicks.hours(2), 32);

    // Gravel and sand are sparse surface resources; a small cap avoids cluttering memory.
    public static final MemoryType.DecayingSpatialMemoryType GRAVEL_SITES = MemoryType.decaying(
            "gravel_sites", ClockTicks.hours(2), 6);

    public static final MemoryType.DecayingSpatialMemoryType SAND_SITES = MemoryType.decaying(
            "sand_sites", ClockTicks.hours(2), 6);

    public static final MemoryType.VanillaMemoryType<List<UUID>> WILLING_COURTSHIP_PARTNERS = MemoryType.vanillaBacked(
            "willing_courtship_partners", MemoryModuleTypeRegistry.WILLING_COURTSHIP_PARTNERS);

    public static final MemoryType.VanillaMemoryType<PerceivedEntities> NEARBY_SENSED_ENTITIES = MemoryType.vanillaBacked(
            "nearby_sensed_entities", MemoryModuleTypeRegistry.NEARBY_SENSED_ENTITIES);

    /**
     * All decaying spatial memories, in declaration order. Used by diagnostics (e.g. the
     * {@code /stest memory} dump) and any future bulk operation over the decaying store.
     */
    public static List<MemoryType.DecayingSpatialMemoryType> decayingSpatialTypes() {
        return List.of(
                RIPE_MELON_SITES,
                RIPE_PUMPKIN_SITES,
                RIPE_SWEET_BERRY_BUSH_SITES,
                RIPE_CROP_SITES,
                NETHER_WART_FARM_SITES,
                HARVESTABLE_SUGARCANE_SITES,
                FULL_HIVE_SITES,
                ORE_SITES,
                GRAVEL_SITES,
                SAND_SITES);
    }

}
