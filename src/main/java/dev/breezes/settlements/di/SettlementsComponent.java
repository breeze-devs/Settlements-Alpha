package dev.breezes.settlements.di;

import dagger.Component;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.di.modules.BehaviorServicesModule;
import dev.breezes.settlements.di.modules.ConfigModule;
import dev.breezes.settlements.di.modules.CraftingModule;
import dev.breezes.settlements.di.modules.DataManagerModule;
import dev.breezes.settlements.di.modules.EconomyModule;
import dev.breezes.settlements.di.modules.ReloadListenerModule;
import dev.breezes.settlements.di.modules.WorldGenerationModule;
import dev.breezes.settlements.domain.generation.pipeline.GenerationPipeline;
import dev.breezes.settlements.infrastructure.minecraft.data.building.BuildingDefinitionDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.farming.hive.CollectHoneyYieldDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.farming.crops.CultivationCropDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.farming.hive.HarvestHoneycombYieldDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.scoring.TraitScorerDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.traits.TraitDefinitionDataManager;
import dev.breezes.settlements.infrastructure.minecraft.worldgen.NbtTemplateResolver;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import javax.inject.Singleton;
import java.util.Set;

@Singleton
@Component(modules = {
        ConfigModule.class,
        DataManagerModule.class,
        BehaviorServicesModule.class,
        EconomyModule.class,
        CraftingModule.class,
        WorldGenerationModule.class,
        ReloadListenerModule.class,
})
public interface SettlementsComponent {

    GenerationPipeline generationPipeline();

    TraitDefinitionDataManager traitDefinitionDataManager();

    TraitScorerDataManager traitScorerDataManager();

    BuildingDefinitionDataManager buildingDefinitionDataManager();

    CollectHoneyYieldDataManager collectHoneyYieldDataManager();

    CultivationCropDataManager cultivationCropDataManager();

    HarvestHoneycombYieldDataManager harvestHoneycombYieldDataManager();

    NbtTemplateResolver nbtTemplateResolver();

    HungerConfig hungerConfig();

    @DataReloadListeners
    Set<PreparableReloadListener> dataReloadListeners();

    @PostReloadListeners
    Set<PreparableReloadListener> postReloadListeners();

    ServerComponent.Factory serverComponentFactory();

    ClientComponent.Factory clientComponentFactory();

}
