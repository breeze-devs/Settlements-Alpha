package dev.breezes.settlements.di.modules;

import dagger.Module;
import dagger.Provides;
import dev.breezes.settlements.application.enchanting.engine.EnchantmentEngine;
import dev.breezes.settlements.infrastructure.minecraft.data.building.BuildingDefinitionDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.enchanting.EnchantmentCostDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.enchanting.SpecializationDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.fishing.FishCatchDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.history.HistoryEventDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.scoring.TraitScorerDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.survey.BiomeSurveyDataManager;
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
    static EnchantmentEngine enchantmentEngine(EnchantmentCostDataManager costDataManager) {
        // TODO: move EnchantmentEngine to its own module — it's an application-layer service, not a data manager.
        return new EnchantmentEngine(costDataManager);
    }

}
