package dev.breezes.settlements.di;

import dagger.Component;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.bootstrap.event.GenerationDataValidationReloadListener;
import dev.breezes.settlements.bootstrap.event.SettlementTemplateReloadListener;
import dev.breezes.settlements.di.modules.BehaviorServicesModule;
import dev.breezes.settlements.di.modules.ConfigModule;
import dev.breezes.settlements.di.modules.DataManagerModule;
import dev.breezes.settlements.di.modules.GenerationModule;
import dev.breezes.settlements.domain.generation.pipeline.GenerationPipeline;
import dev.breezes.settlements.infrastructure.minecraft.data.building.BuildingDefinitionDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.enchanting.EnchantmentCostDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.enchanting.SpecializationDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.farming.hive.CollectHoneyYieldDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.farming.hive.HarvestHoneycombYieldDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.fishing.FishCatchDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.history.HistoryEventDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.scoring.TraitScorerDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.survey.BiomeSurveyDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.traits.TraitDefinitionDataManager;

import javax.inject.Singleton;

@Singleton
@Component(modules = {
        ConfigModule.class,
        DataManagerModule.class,
        BehaviorServicesModule.class,
        GenerationModule.class,
})
public interface SettlementsComponent {

    GenerationPipeline generationPipeline();

    EnchantmentCostDataManager enchantmentCostDataManager();

    SpecializationDataManager specializationDataManager();

    FishCatchDataManager fishCatchDataManager();

    BiomeSurveyDataManager biomeSurveyDataManager();

    TraitDefinitionDataManager traitDefinitionDataManager();

    TraitScorerDataManager traitScorerDataManager();

    HistoryEventDataManager historyEventDataManager();

    BuildingDefinitionDataManager buildingDefinitionDataManager();

    CollectHoneyYieldDataManager collectHoneyYieldDataManager();

    HarvestHoneycombYieldDataManager harvestHoneycombYieldDataManager();

    GenerationDataValidationReloadListener generationDataValidationReloadListener();

    SettlementTemplateReloadListener settlementTemplateReloadListener();

    HungerConfig hungerConfig();

    ServerComponent.Factory serverComponentFactory();

    ClientComponent.Factory clientComponentFactory();

}
