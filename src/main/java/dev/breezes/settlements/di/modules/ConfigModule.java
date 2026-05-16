package dev.breezes.settlements.di.modules;

import dagger.Module;
import dagger.Provides;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.BreedAnimalsConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.ShearSheepConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.TameCatConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.TameWolfConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.butchering.ButcherLivestockConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.milking.MilkCowConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.washing.WashWolfConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.cooking.smokemeat.SmokeMeatConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.crafting.CutStoneConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.enchanting.EnchantItemConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.CollectHoneyConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestHoneycombConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestOreConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestSoulSandConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestSugarCaneConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.fishing.FishingConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.idle.RingBellConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.idle.WalkDogConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.smelting.blastore.BlastOreConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.support.RepairIronGolemConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.support.ThrowPotionsConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.wolf.walkdog.WolfWalkConfig;
import dev.breezes.settlements.application.ai.trading.TradingConfig;
import dev.breezes.settlements.application.hunger.HungerConfig;
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
    static HarvestSoulSandConfig harvestSoulSandConfig() {
        return ConfigFactory.create(HarvestSoulSandConfig.class);
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
    static HungerConfig hungerConfig() {
        return ConfigFactory.create(HungerConfig.class);
    }

    @Provides
    @Singleton
    static TradingConfig tradingConfig() {
        return ConfigFactory.create(TradingConfig.class);
    }

}
