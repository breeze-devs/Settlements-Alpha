package dev.breezes.settlements.domain.generation.building;

import dev.breezes.settlements.domain.generation.model.building.BuildingDefinition;
import dev.breezes.settlements.domain.generation.model.building.DisplayInfo;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;
import java.util.Optional;

public interface BuildingRegistry {

    List<BuildingDefinition> allBuildings();
 
    List<BuildingDefinition> constrainedBuildings();

    List<BuildingDefinition> unconstrainedBuildings();

    List<BuildingDefinition> forTrait(TraitId trait);

    Optional<BuildingDefinition> byId(String id);

    @Nonnull
    default String displayNameFor(@Nonnull String buildingDefinitionId) {
        return this.byId(buildingDefinitionId)
                .map(BuildingDefinition::displayInfo)
                .map(displayInfo -> displayNameOrFallback(displayInfo, buildingDefinitionId))
                .orElse(buildingDefinitionId);
    }

    @Nonnull
    private static String displayNameOrFallback(@Nullable DisplayInfo displayInfo, @Nonnull String fallback) {
        if (displayInfo == null || displayInfo.displayName() == null || displayInfo.displayName().isBlank()) {
            return fallback;
        }
        return displayInfo.displayName();
    }

}
