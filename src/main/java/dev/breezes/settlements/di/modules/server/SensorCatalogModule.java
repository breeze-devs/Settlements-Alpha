package dev.breezes.settlements.di.modules.server;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import dagger.multibindings.Multibinds;
import dev.breezes.settlements.application.ai.sensors.BlockResource;
import dev.breezes.settlements.application.ai.sensors.BlockResourceSensor;
import dev.breezes.settlements.application.ai.sensors.BlockResourceSensorConfig;
import dev.breezes.settlements.application.ai.sensors.EntityPerceptionSensor;
import dev.breezes.settlements.application.ai.sensors.EntityPerceptionSensorConfig;
import dev.breezes.settlements.application.ai.sensors.WorldResourceIndex;
import dev.breezes.settlements.di.catalog.VillagerSensorFactory;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.settlement.query.SettlementQueryService;
import dev.breezes.settlements.domain.world.blocks.BlockMatchers;

import java.util.Set;

@Module
public abstract class SensorCatalogModule {

    @Multibinds
    abstract Set<VillagerSensorFactory> villagerSensorFactories();

    @Multibinds
    abstract Set<BlockResource> blockResources();

    // TODO: sensor assignment is currently universal — every villager ticks every registered sensor.
    //  Decide the long-term model: per-profession pools (mirroring PoolModule for behaviors) would cut
    //  the per-tick big-scan cost at the 1000+ NPC target, but universal sensing may be correct for the
    //  LLM planner, whose structured world-model is meant to be role-agnostic (design doc D4, open
    //  questions #3 sensor-sharing-granularity and #7 registration-mechanism).
    @Provides
    @IntoSet
    static VillagerSensorFactory blockResourceSensor(BlockResourceSensorConfig config,
                                                     Set<BlockResource> resources,
                                                     WorldResourceIndex index,
                                                     SettlementQueryService settlementQueryService) {
        return villager -> new BlockResourceSensor(config, resources, index, settlementQueryService);
    }

    @Provides
    @IntoSet
    static VillagerSensorFactory entityPerceptionSensor(EntityPerceptionSensorConfig config) {
        return villager -> new EntityPerceptionSensor(config, villager);
    }

    @Provides
    @IntoSet
    static BlockResource ripePumpkin() {
        return new BlockResource(BlockMatchers.HARVESTABLE_PUMPKIN, MemoryTypeRegistry.RIPE_PUMPKIN_SITES);
    }

    @Provides
    @IntoSet
    static BlockResource ripeMelon() {
        return new BlockResource(BlockMatchers.HARVESTABLE_MELON, MemoryTypeRegistry.RIPE_MELON_SITES);
    }

    @Provides
    @IntoSet
    static BlockResource ripeSweetBerryBush() {
        return new BlockResource(BlockMatchers.RIPE_SWEET_BERRY_BUSH, MemoryTypeRegistry.RIPE_SWEET_BERRY_BUSH_SITES);
    }

    @Provides
    @IntoSet
    static BlockResource ripeCrop() {
        return new BlockResource(BlockMatchers.RIPE_CROP, MemoryTypeRegistry.RIPE_CROP_SITES);
    }

    @Provides
    @IntoSet
    static BlockResource netherWartFarm() {
        return new BlockResource(BlockMatchers.HARVESTABLE_NETHER_WART, MemoryTypeRegistry.NETHER_WART_FARM_SITES);
    }

    @Provides
    @IntoSet
    static BlockResource harvestableSugarcane() {
        return new BlockResource(BlockMatchers.HARVESTABLE_SUGARCANE, MemoryTypeRegistry.HARVESTABLE_SUGARCANE_SITES);
    }

    @Provides
    @IntoSet
    static BlockResource fullHive() {
        return new BlockResource(BlockMatchers.FULL_HIVE, MemoryTypeRegistry.FULL_HIVE_SITES);
    }

    @Provides
    @IntoSet
    static BlockResource ore() {
        return new BlockResource(BlockMatchers.HARVESTABLE_ORE, MemoryTypeRegistry.ORE_SITES);
    }

    @Provides
    @IntoSet
    static BlockResource gravel() {
        return new BlockResource(BlockMatchers.LOOSE_GRAVEL, MemoryTypeRegistry.GRAVEL_SITES);
    }

    @Provides
    @IntoSet
    static BlockResource sand() {
        return new BlockResource(BlockMatchers.LOOSE_SAND, MemoryTypeRegistry.SAND_SITES);
    }

    // Cultivation totems are discovered by the dedicated CultivationTotemSensor (block-entity scan at a
    // larger range), not the generic block-resource sensor — so there is no BlockResource entry here.

}
