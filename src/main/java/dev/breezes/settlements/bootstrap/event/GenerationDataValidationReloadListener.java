package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.infrastructure.minecraft.data.building.BuildingDefinitionDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.scoring.TraitScorerDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.traits.TraitDefinitionDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.validation.GenerationDataValidator;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
@CustomLog
public final class GenerationDataValidationReloadListener extends SimplePreparableReloadListener<Void> {

    private final GenerationDataValidator validator = new GenerationDataValidator();

    private final TraitDefinitionDataManager traitDefinitionDataManager;
    private final TraitScorerDataManager traitScorerDataManager;
    private final BuildingDefinitionDataManager buildingDefinitionDataManager;

    @Override
    protected Void prepare(@Nonnull ResourceManager resourceManager, @Nonnull ProfilerFiller profiler) {
        return null;
    }

    @Override
    protected void apply(Void ignored, @Nonnull ResourceManager resourceManager, @Nonnull ProfilerFiller profiler) {
        log.info("Validating generation datapack cross-registry references");
        this.validator.validateAndApply(
                this.traitDefinitionDataManager,
                this.traitScorerDataManager,
                this.buildingDefinitionDataManager
        );
    }

}
