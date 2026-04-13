package dev.breezes.settlements.di;

import dagger.Component;
import dev.breezes.settlements.domain.generation.pipeline.GenerationPipeline;
import dev.breezes.settlements.infrastructure.minecraft.data.building.BuildingDefinitionDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.history.HistoryEventDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.scoring.TraitScorerDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.survey.BiomeSurveyDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.traits.TraitDefinitionDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.validation.GenerationDataValidator;

import javax.inject.Singleton;

@Singleton
@Component(modules = {
        TestConfigModule.class,
        TestDataManagerModule.class,
})
public interface TestSettlementsComponent {

    GenerationPipeline generationPipeline();

    GenerationDataValidator generationDataValidator();

    BuildingDefinitionDataManager buildingDefinitionDataManager();

    BiomeSurveyDataManager biomeSurveyDataManager();

    TraitScorerDataManager traitScorerDataManager();

    TraitDefinitionDataManager traitDefinitionDataManager();

    HistoryEventDataManager historyEventDataManager();

}
