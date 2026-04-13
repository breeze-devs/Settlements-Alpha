package dev.breezes.settlements.di;

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
import dev.breezes.settlements.infrastructure.minecraft.data.validation.GenerationDataValidator;

import javax.inject.Singleton;

/**
 * Provides a small real-object test graph for generation and data-validation.
 * <p>
 * We intentionally start with concrete in-memory-capable managers because these
 * tests mostly validate wiring and registry interactions rather than expensive
 * external infrastructure.
 */
@Module
public final class TestDataManagerModule {

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
    static BuildingRegistry buildingRegistry(BuildingDefinitionDataManager manager) {
        return manager;
    }

    @Provides
    @Singleton
    static BiomeSurveyLookup biomeSurveyLookup(BiomeSurveyDataManager manager) {
        return manager;
    }

    @Provides
    @Singleton
    static TraitScorerRegistry traitScorerRegistry(TraitScorerDataManager manager) {
        return manager;
    }

    @Provides
    @Singleton
    static TraitRegistry traitRegistry(TraitDefinitionDataManager manager) {
        return manager;
    }

    @Provides
    @Singleton
    static HistoryEventRegistry historyEventRegistry(HistoryEventDataManager manager) {
        return manager;
    }

    @Provides
    @Singleton
    static GenerationPipeline generationPipeline(BiomeSurveyLookup biomeLookup,
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

    @Provides
    @Singleton
    static GenerationDataValidator generationDataValidator() {
        return new GenerationDataValidator();
    }

}
