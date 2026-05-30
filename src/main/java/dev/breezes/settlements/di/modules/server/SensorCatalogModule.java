package dev.breezes.settlements.di.modules.server;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import dagger.multibindings.Multibinds;
import dev.breezes.settlements.application.ai.sensors.BlockResource;
import dev.breezes.settlements.application.ai.sensors.BlockResourceSensor;
import dev.breezes.settlements.application.ai.sensors.BlockResourceSensorConfig;
import dev.breezes.settlements.di.catalog.VillagerSensorFactory;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
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
    static VillagerSensorFactory blockResourceSensor(BlockResourceSensorConfig config, Set<BlockResource> resources) {
        return villager -> new BlockResourceSensor(config, resources);
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

}
