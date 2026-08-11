package dev.breezes.settlements.di.modules.server;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import dagger.multibindings.Multibinds;
import dev.breezes.settlements.application.ai.inference.InferenceGate;
import dev.breezes.settlements.application.ai.sensors.BlockResource;
import dev.breezes.settlements.application.ai.sensors.BlockResourceSensor;
import dev.breezes.settlements.application.ai.sensors.BlockResourceSensorConfig;
import dev.breezes.settlements.application.ai.sensors.DemandedGroundItemSensor;
import dev.breezes.settlements.application.ai.sensors.DemandedGroundItemSensorConfig;
import dev.breezes.settlements.application.ai.sensors.EntityPerceptionSensor;
import dev.breezes.settlements.application.ai.sensors.EntityPerceptionSensorConfig;
import dev.breezes.settlements.application.ai.sensors.EntitySightingEmitterSensor;
import dev.breezes.settlements.application.ai.sensors.WorldResourceIndex;
import dev.breezes.settlements.application.economy.demand.DemandEvaluator;
import dev.breezes.settlements.di.BaseLane;
import dev.breezes.settlements.di.CognitionScoped;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.di.catalog.VillagerSensorFactory;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventEmitter;
import dev.breezes.settlements.domain.settlement.query.SettlementQueryService;
import dev.breezes.settlements.domain.world.blocks.BlockMatchers;

import java.util.HashSet;
import java.util.Set;

@Module
public abstract class SensorCatalogModule {

    /**
     * Sensors that run regardless of the SIS kill-switch.
     */
    @Multibinds
    @BaseLane
    abstract Set<VillagerSensorFactory> baseVillagerSensorFactories();

    /**
     * Sensors that only make sense with the SIS cognition lane on.
     */
    @Multibinds
    @CognitionScoped
    abstract Set<VillagerSensorFactory> cognitionVillagerSensorFactories();

    @Multibinds
    abstract Set<BlockResource> blockResources();

    @Provides
    @IntoSet
    @BaseLane
    static VillagerSensorFactory blockResourceSensor(BlockResourceSensorConfig config,
                                                     Set<BlockResource> resources,
                                                     WorldResourceIndex index,
                                                     SettlementQueryService settlementQueryService) {
        return villager -> new BlockResourceSensor(config, resources, index, settlementQueryService);
    }

    @Provides
    @IntoSet
    @BaseLane
    static VillagerSensorFactory entityPerceptionSensor(EntityPerceptionSensorConfig config) {
        return villager -> new EntityPerceptionSensor(config, villager);
    }

    @Provides
    @IntoSet
    @CognitionScoped
    static VillagerSensorFactory entitySightingEmitterSensor(WorldEventEmitter emitter) {
        return villager -> new EntitySightingEmitterSensor(emitter, villager);
    }

    @Provides
    @IntoSet
    @BaseLane
    static VillagerSensorFactory demandedGroundItemSensor(DemandedGroundItemSensorConfig config, DemandEvaluator demandEvaluator) {
        return villager -> new DemandedGroundItemSensor(config, demandEvaluator, villager);
    }

    /**
     * The effective sensor set {@link dev.breezes.settlements.application.ai.brain.VillagerBrain}
     * consumes — always the base lane, plus the cognition lane only when {@link InferenceGate} is
     * on. This is the sole unqualified {@code Set<VillagerSensorFactory>} binding; every consumer
     * injects it directly and never sees the {@link BaseLane}/{@link CognitionScoped} split.
     * <p>
     * The gate is a load-time snapshot (restart-only application), so this merge is effectively
     * computed once per server session despite running on every villager's brain initialization.
     */
    @Provides
    @ServerScope
    static Set<VillagerSensorFactory> villagerSensorFactories(@BaseLane Set<VillagerSensorFactory> baseFactories,
                                                              @CognitionScoped Set<VillagerSensorFactory> cognitionFactories,
                                                              InferenceGate inferenceGate) {
        if (!inferenceGate.isEnabled()) {
            return baseFactories;
        }

        Set<VillagerSensorFactory> merged = new HashSet<>(baseFactories);
        merged.addAll(cognitionFactories);
        return Set.copyOf(merged);
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

    @Provides
    @IntoSet
    static BlockResource scavengeableFlora() {
        return new BlockResource(BlockMatchers.SCAVENGEABLE_FLORA, MemoryTypeRegistry.SCAVENGEABLE_FLORA_SITES);
    }

    @Provides
    @IntoSet
    static BlockResource anvil() {
        return new BlockResource(BlockMatchers.ANVIL, MemoryTypeRegistry.ANVIL_SITES);
    }

    // Cultivation lilies are discovered by the dedicated CultivationSiteSensor (block-entity scan at a
    // larger range), not the generic block-resource sensor — so there is no BlockResource entry here.

}
