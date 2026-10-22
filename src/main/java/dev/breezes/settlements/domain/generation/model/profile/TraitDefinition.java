package dev.breezes.settlements.domain.generation.model.profile;

import dev.breezes.settlements.domain.generation.model.building.DisplayInfo;
import lombok.Builder;

import javax.annotation.Nullable;

@Builder
public record TraitDefinition(
        TraitId id,
        @Nullable DisplayInfo displayInfo
) {
}
