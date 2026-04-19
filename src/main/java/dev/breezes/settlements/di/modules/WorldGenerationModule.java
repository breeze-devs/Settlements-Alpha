package dev.breezes.settlements.di.modules;

import dagger.Module;
import dagger.Provides;
import dev.breezes.settlements.domain.generation.building.BuildingRegistry;
import dev.breezes.settlements.domain.generation.history.HistoryEventRegistry;
import dev.breezes.settlements.domain.generation.pipeline.GenerationPipeline;
import dev.breezes.settlements.domain.generation.scoring.TraitScorerRegistry;
import dev.breezes.settlements.domain.generation.survey.BiomeSurveyLookup;
import dev.breezes.settlements.domain.generation.trait.TraitRegistry;
import dev.breezes.settlements.infrastructure.minecraft.data.building.BuildingDefinitionDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.history.HistoryEventDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.scoring.TraitScorerDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.survey.BiomeSurveyDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.traits.TraitDefinitionDataManager;

import javax.inject.Singleton;

/**
 * Exposes world-generation registries through domain interfaces so callers can
 * depend on stable abstractions instead of infrastructure details.
 */
@Module
public final class WorldGenerationModule {

    @Provides
    @Singleton
    static BuildingRegistry buildingRegistry(BuildingDefinitionDataManager buildingDefinitionDataManager) {
        return buildingDefinitionDataManager;
    }

    @Provides
    @Singleton
    static BiomeSurveyLookup biomeSurveyLookup(BiomeSurveyDataManager biomeSurveyDataManager) {
        return biomeSurveyDataManager;
    }

    @Provides
    @Singleton
    static TraitScorerRegistry traitScorerRegistry(TraitScorerDataManager traitScorerDataManager) {
        return traitScorerDataManager;
    }

    @Provides
    @Singleton
    static TraitRegistry traitRegistry(TraitDefinitionDataManager traitDefinitionDataManager) {
        return traitDefinitionDataManager;
    }

    @Provides
    @Singleton
    static HistoryEventRegistry historyEventRegistry(HistoryEventDataManager historyEventDataManager) {
        return historyEventDataManager;
    }

    @Provides
    @Singleton
    static GenerationPipeline generationPipeline(
            BiomeSurveyLookup biomeLookup,
            TraitScorerRegistry traitScorerRegistry,
            BuildingRegistry buildingRegistry,
            HistoryEventRegistry historyEventRegistry,
            TraitRegistry traitRegistry) {
        return new GenerationPipeline(
                biomeLookup,
                traitScorerRegistry,
                buildingRegistry,
                historyEventRegistry,
                traitRegistry);
    }

}
