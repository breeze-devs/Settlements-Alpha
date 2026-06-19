package dev.breezes.settlements.di.modules;

import dagger.Module;
import dagger.Provides;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.BreedAnimalsConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.ShearSheepConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.TameCatConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.TameWolfConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.butchering.ButcherLivestockConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.feeding.FeedWolfConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.milking.MilkCowConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.washing.WashWolfConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.cartographer.SurveyLandscapeConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.cooking.smokemeat.SmokeMeatConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.courtship.CourtshipInitiateConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.crafting.CutStoneConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.enchanting.EnchantItemConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.CollectHoneyConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestHoneycombConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestMelonConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestNetherWartConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestOreConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestPumpkinConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestRipeCropsConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestSugarCaneConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestSweetBerriesConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.fishing.FishingConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.idle.WalkDogConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.investigate.InvestigateConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.leatherworking.dyeleather.DyeLeatherConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.leatherworking.washleather.WashLeatherConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.logistics.CollectDemandedItemConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.logistics.TakeFromChestConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.nitwit.RingBellConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.nitwit.ThrowEggsConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.smelting.blastore.BlastOreConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.support.RepairIronGolemConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.support.ThrowPotionsConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.wolf.walkdog.WolfWalkConfig;
import dev.breezes.settlements.application.ai.dialogue.DialogueConfig;
import dev.breezes.settlements.application.ai.inference.InferenceConfig;
import dev.breezes.settlements.application.ai.sensors.BlockResourceSensorConfig;
import dev.breezes.settlements.application.ai.sensors.EntityPerceptionSensorConfig;
import dev.breezes.settlements.application.ai.trading.TradingConfig;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.bootstrap.event.VillageAnimalSpawnerConfig;
import dev.breezes.settlements.domain.ai.eventlane.EventLaneConfig;
import dev.breezes.settlements.infrastructure.config.factory.ConfigFactory;

import javax.inject.Singleton;

/**
 * Exposes immutable startup config snapshots to the Dagger graph.
 * <p>
 * NeoForge still owns config discovery and loading. This module only bridges the
 * already-materialized records from {@link ConfigFactory} into compile-time DI.
 */
@Module
public final class ConfigModule {

    @Provides
    @Singleton
    static SurveyLandscapeConfig cartographerSurveyConfig() {
        return ConfigFactory.create(SurveyLandscapeConfig.class);
    }

    @Provides
    @Singleton
    static FishingConfig fishingConfig() {
        return ConfigFactory.create(FishingConfig.class);
    }

    @Provides
    @Singleton
    static ShearSheepConfig shearSheepConfig() {
        return ConfigFactory.create(ShearSheepConfig.class);
    }

    @Provides
    @Singleton
    static TameCatConfig tameCatConfig() {
        return ConfigFactory.create(TameCatConfig.class);
    }

    @Provides
    @Singleton
    static TameWolfConfig tameWolfConfig() {
        return ConfigFactory.create(TameWolfConfig.class);
    }

    @Provides
    @Singleton
    static WashWolfConfig washWolfConfig() {
        return ConfigFactory.create(WashWolfConfig.class);
    }

    @Provides
    @Singleton
    static FeedWolfConfig feedWolfConfig() {
        return ConfigFactory.create(FeedWolfConfig.class);
    }

    @Provides
    @Singleton
    static WolfWalkConfig wolfWalkConfig() {
        return ConfigFactory.create(WolfWalkConfig.class);
    }

    @Provides
    @Singleton
    static WalkDogConfig walkDogConfig() {
        return ConfigFactory.create(WalkDogConfig.class);
    }

    @Provides
    @Singleton
    static RingBellConfig ringBellConfig() {
        return ConfigFactory.create(RingBellConfig.class);
    }

    @Provides
    @Singleton
    static CourtshipInitiateConfig courtshipInitiateConfig() {
        return ConfigFactory.create(CourtshipInitiateConfig.class);
    }

    @Provides
    @Singleton
    static InvestigateConfig investigateConfig() {
        return ConfigFactory.create(InvestigateConfig.class);
    }

    @Provides
    @Singleton
    static BreedAnimalsConfig breedAnimalsConfig() {
        return ConfigFactory.create(BreedAnimalsConfig.class);
    }

    @Provides
    @Singleton
    static ButcherLivestockConfig butcherLivestockConfig() {
        return ConfigFactory.create(ButcherLivestockConfig.class);
    }

    @Provides
    @Singleton
    static MilkCowConfig milkCowConfig() {
        return ConfigFactory.create(MilkCowConfig.class);
    }

    @Provides
    @Singleton
    static SmokeMeatConfig smokeMeatConfig() {
        return ConfigFactory.create(SmokeMeatConfig.class);
    }

    @Provides
    @Singleton
    static BlastOreConfig blastOreConfig() {
        return ConfigFactory.create(BlastOreConfig.class);
    }

    @Provides
    @Singleton
    static CutStoneConfig cutStoneConfig() {
        return ConfigFactory.create(CutStoneConfig.class);
    }

    @Provides
    @Singleton
    static EnchantItemConfig enchantItemConfig() {
        return ConfigFactory.create(EnchantItemConfig.class);
    }

    @Provides
    @Singleton
    static WashLeatherConfig washLeatherConfig() {
        return ConfigFactory.create(WashLeatherConfig.class);
    }

    @Provides
    @Singleton
    static DyeLeatherConfig dyeLeatherConfig() {
        return ConfigFactory.create(DyeLeatherConfig.class);
    }

    @Provides
    @Singleton
    static HarvestSugarCaneConfig harvestSugarCaneConfig() {
        return ConfigFactory.create(HarvestSugarCaneConfig.class);
    }

    @Provides
    @Singleton
    static CollectHoneyConfig collectHoneyConfig() {
        return ConfigFactory.create(CollectHoneyConfig.class);
    }

    @Provides
    @Singleton
    static HarvestHoneycombConfig harvestHoneycombConfig() {
        return ConfigFactory.create(HarvestHoneycombConfig.class);
    }

    @Provides
    @Singleton
    static HarvestPumpkinConfig harvestPumpkinConfig() {
        return ConfigFactory.create(HarvestPumpkinConfig.class);
    }

    @Provides
    @Singleton
    static BlockResourceSensorConfig blockResourceSensorConfig() {
        return ConfigFactory.create(BlockResourceSensorConfig.class);
    }

    @Provides
    @Singleton
    static EntityPerceptionSensorConfig entityPerceptionSensorConfig() {
        return ConfigFactory.create(EntityPerceptionSensorConfig.class);
    }

    @Provides
    @Singleton
    static HarvestMelonConfig harvestMelonConfig() {
        return ConfigFactory.create(HarvestMelonConfig.class);
    }

    @Provides
    @Singleton
    static HarvestSweetBerriesConfig harvestSweetBerriesConfig() {
        return ConfigFactory.create(HarvestSweetBerriesConfig.class);
    }

    @Provides
    @Singleton
    static HarvestRipeCropsConfig harvestRipeCropsConfig() {
        return ConfigFactory.create(HarvestRipeCropsConfig.class);
    }

    @Provides
    @Singleton
    static HarvestNetherWartConfig harvestNetherWartConfig() {
        return ConfigFactory.create(HarvestNetherWartConfig.class);
    }

    @Provides
    @Singleton
    static HarvestOreConfig harvestOreConfig() {
        return ConfigFactory.create(HarvestOreConfig.class);
    }

    @Provides
    @Singleton
    static RepairIronGolemConfig repairIronGolemConfig() {
        return ConfigFactory.create(RepairIronGolemConfig.class);
    }

    @Provides
    @Singleton
    static ThrowPotionsConfig throwPotionsConfig() {
        return ConfigFactory.create(ThrowPotionsConfig.class);
    }

    @Provides
    @Singleton
    static ThrowEggsConfig throwEggsConfig() {
        return ConfigFactory.create(ThrowEggsConfig.class);
    }

    @Provides
    @Singleton
    static HungerConfig hungerConfig() {
        return ConfigFactory.create(HungerConfig.class);
    }

    @Provides
    @Singleton
    static TradingConfig tradingConfig() {
        return ConfigFactory.create(TradingConfig.class);
    }

    @Provides
    @Singleton
    static TakeFromChestConfig takeFromChestConfig() {
        return ConfigFactory.create(TakeFromChestConfig.class);
    }

    @Provides
    @Singleton
    static CollectDemandedItemConfig collectDemandedItemConfig() {
        return ConfigFactory.create(CollectDemandedItemConfig.class);
    }

    @Provides
    @Singleton
    static DialogueConfig dialogueConfig() {
        return ConfigFactory.create(DialogueConfig.class);
    }

    @Provides
    @Singleton
    static InferenceConfig inferenceConfig() {
        return ConfigFactory.create(InferenceConfig.class);
    }

    @Provides
    @Singleton
    static EventLaneConfig eventLaneConfig() {
        return ConfigFactory.create(EventLaneConfig.class);
    }

    @Provides
    @Singleton
    static VillageAnimalSpawnerConfig villageAnimalSpawnerConfig() {
        return ConfigFactory.create(VillageAnimalSpawnerConfig.class);
    }

}
