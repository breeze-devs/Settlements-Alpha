package dev.breezes.settlements.di.modules;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import dev.breezes.settlements.bootstrap.event.CraftCatalogValidationReloadListener;
import dev.breezes.settlements.bootstrap.event.GenerationDataValidationReloadListener;
import dev.breezes.settlements.di.DataReloadListeners;
import dev.breezes.settlements.di.PostReloadListeners;
import dev.breezes.settlements.infrastructure.minecraft.data.building.BuildingDefinitionDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.crafting.CraftCatalogDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.enchanting.EnchantmentCostDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.enchanting.SpecializationDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.farming.crops.CultivationCropDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.farming.hive.CollectHoneyYieldDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.farming.hive.HarvestHoneycombYieldDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.fishing.FishCatchDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.history.HistoryEventDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.mason.ExcavateSubstrateYieldDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.mining.OreRegenDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.scavenge.ScavengeYieldDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.scoring.TraitScorerDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.survey.BiomeSurveyDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.trading.TradeCatalogDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.traits.TraitDefinitionDataManager;
import dev.breezes.settlements.infrastructure.minecraft.worldgen.NbtTemplateResolver;
import net.minecraft.server.packs.resources.PreparableReloadListener;

/**
 * Multibinds every datapack reload listener into two Dagger sets so registration can never silently
 * miss one: {@link DataReloadListeners} (producers, mutually order-independent) is drained fully before
 * {@link PostReloadListeners} (validators/consumers that read the populated producers). Adding a new
 * listener from now on means adding one {@code @Binds} method here, not editing an event handler.
 */
@Module
public interface ReloadListenerModule {

    @Binds
    @IntoSet
    @DataReloadListeners
    PreparableReloadListener enchantmentCostDataManager(EnchantmentCostDataManager dataManager);

    @Binds
    @IntoSet
    @DataReloadListeners
    PreparableReloadListener specializationDataManager(SpecializationDataManager dataManager);

    @Binds
    @IntoSet
    @DataReloadListeners
    PreparableReloadListener fishCatchDataManager(FishCatchDataManager dataManager);

    @Binds
    @IntoSet
    @DataReloadListeners
    PreparableReloadListener biomeSurveyDataManager(BiomeSurveyDataManager dataManager);

    @Binds
    @IntoSet
    @DataReloadListeners
    PreparableReloadListener traitDefinitionDataManager(TraitDefinitionDataManager dataManager);

    @Binds
    @IntoSet
    @DataReloadListeners
    PreparableReloadListener traitScorerDataManager(TraitScorerDataManager dataManager);

    @Binds
    @IntoSet
    @DataReloadListeners
    PreparableReloadListener historyEventDataManager(HistoryEventDataManager dataManager);

    @Binds
    @IntoSet
    @DataReloadListeners
    PreparableReloadListener buildingDefinitionDataManager(BuildingDefinitionDataManager dataManager);

    @Binds
    @IntoSet
    @DataReloadListeners
    PreparableReloadListener collectHoneyYieldDataManager(CollectHoneyYieldDataManager dataManager);

    @Binds
    @IntoSet
    @DataReloadListeners
    PreparableReloadListener cultivationCropDataManager(CultivationCropDataManager dataManager);

    @Binds
    @IntoSet
    @DataReloadListeners
    PreparableReloadListener harvestHoneycombYieldDataManager(HarvestHoneycombYieldDataManager dataManager);

    @Binds
    @IntoSet
    @DataReloadListeners
    PreparableReloadListener excavateSubstrateYieldDataManager(ExcavateSubstrateYieldDataManager dataManager);

    @Binds
    @IntoSet
    @DataReloadListeners
    PreparableReloadListener tradeCatalogDataManager(TradeCatalogDataManager dataManager);

    @Binds
    @IntoSet
    @DataReloadListeners
    PreparableReloadListener craftCatalogDataManager(CraftCatalogDataManager dataManager);

    @Binds
    @IntoSet
    @DataReloadListeners
    PreparableReloadListener oreRegenDataManager(OreRegenDataManager dataManager);

    @Binds
    @IntoSet
    @DataReloadListeners
    PreparableReloadListener scavengeYieldDataManager(ScavengeYieldDataManager dataManager);

    @Binds
    @IntoSet
    @DataReloadListeners
    PreparableReloadListener nbtTemplateResolver(NbtTemplateResolver resolver);

    @Binds
    @IntoSet
    @PostReloadListeners
    PreparableReloadListener generationDataValidationReloadListener(GenerationDataValidationReloadListener listener);

    @Binds
    @IntoSet
    @PostReloadListeners
    PreparableReloadListener craftCatalogValidationReloadListener(CraftCatalogValidationReloadListener listener);

}
