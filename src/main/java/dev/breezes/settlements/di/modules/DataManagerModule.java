package dev.breezes.settlements.di.modules;

import dagger.Module;
import dagger.Provides;
import dev.breezes.settlements.infrastructure.minecraft.data.building.BuildingDefinitionDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.crafting.CraftCatalogDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.enchanting.EnchantmentCostDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.enchanting.SpecializationDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.farming.crops.CultivationCropDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.farming.hive.CollectHoneyYieldDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.farming.hive.HarvestHoneycombYieldDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.fishing.FishCatchDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.forge.ForgeCatalogDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.history.HistoryEventDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.mason.ExcavateSubstrateYieldDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.mining.OreRegenDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.scavenge.ScavengeYieldDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.scoring.TraitScorerDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.smelting.BlastOreRecipeDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.survey.BiomeSurveyDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.trading.TradeCatalogDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.traits.TraitDefinitionDataManager;

import javax.inject.Singleton;

@Module
public final class DataManagerModule {

    @Provides
    @Singleton
    static FishCatchDataManager fishCatchDataManager() {
        return new FishCatchDataManager();
    }

    @Provides
    @Singleton
    static EnchantmentCostDataManager enchantmentCostDataManager() {
        return new EnchantmentCostDataManager();
    }

    @Provides
    @Singleton
    static SpecializationDataManager specializationDataManager() {
        return new SpecializationDataManager();
    }

    @Provides
    @Singleton
    static BuildingDefinitionDataManager buildingDefinitionDataManager() {
        return new BuildingDefinitionDataManager();
    }

    @Provides
    @Singleton
    static BiomeSurveyDataManager biomeSurveyDataManager() {
        return new BiomeSurveyDataManager();
    }

    @Provides
    @Singleton
    static TraitScorerDataManager traitScorerDataManager() {
        return new TraitScorerDataManager();
    }

    @Provides
    @Singleton
    static TraitDefinitionDataManager traitDefinitionDataManager() {
        return new TraitDefinitionDataManager();
    }

    @Provides
    @Singleton
    static HistoryEventDataManager historyEventDataManager() {
        return new HistoryEventDataManager();
    }

    @Provides
    @Singleton
    static CollectHoneyYieldDataManager collectHoneyYieldDataManager() {
        return new CollectHoneyYieldDataManager();
    }

    @Provides
    @Singleton
    static CultivationCropDataManager cultivationCropDataManager() {
        return new CultivationCropDataManager();
    }

    @Provides
    @Singleton
    static HarvestHoneycombYieldDataManager harvestHoneycombYieldDataManager() {
        return new HarvestHoneycombYieldDataManager();
    }

    @Provides
    @Singleton
    static ExcavateSubstrateYieldDataManager excavateSubstrateYieldDataManager() {
        return new ExcavateSubstrateYieldDataManager();
    }

    @Provides
    @Singleton
    static TradeCatalogDataManager tradeCatalogDataManager() {
        return new TradeCatalogDataManager();
    }

    @Provides
    @Singleton
    static CraftCatalogDataManager craftCatalogDataManager() {
        return new CraftCatalogDataManager();
    }

    @Provides
    @Singleton
    static ForgeCatalogDataManager forgeCatalogDataManager() {
        return new ForgeCatalogDataManager();
    }

    @Provides
    @Singleton
    static OreRegenDataManager oreRegenDataManager() {
        return new OreRegenDataManager();
    }

    @Provides
    @Singleton
    static ScavengeYieldDataManager scavengeYieldDataManager() {
        return new ScavengeYieldDataManager();
    }

    @Provides
    @Singleton
    static BlastOreRecipeDataManager blastOreRecipeDataManager() {
        return new BlastOreRecipeDataManager();
    }

}
