package dev.breezes.settlements.infrastructure.minecraft.data.validation;

import dev.breezes.settlements.domain.generation.model.building.BuildingDefinition;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.domain.generation.scoring.TraitScorer;
import dev.breezes.settlements.domain.generation.trait.TraitRegistry;
import dev.breezes.settlements.infrastructure.minecraft.data.building.BuildingDefinitionDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.scoring.TraitScorerDataManager;
import lombok.CustomLog;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CustomLog(topic = "resource_loading")
public final class GenerationDataValidator {

    public void validateAndApply(@Nonnull TraitRegistry traitRegistry,
                                 @Nonnull TraitScorerDataManager scorerDataManager,
                                 @Nonnull BuildingDefinitionDataManager buildingDataManager) {
        Set<TraitId> knownTraits = traitRegistry.allTraitIds();

        Map<TraitId, TraitScorer> validScorers = new LinkedHashMap<>();
        for (Map.Entry<TraitId, TraitScorer> entry : scorerDataManager.rawScorers().entrySet()) {
            if (!knownTraits.contains(entry.getKey())) {
                log.warn("Dropping the trait scorer registered for unknown trait '{}'", entry.getKey());
                continue;
            }
            validScorers.put(entry.getKey(), entry.getValue());
        }

        List<BuildingDefinition> validBuildings = new ArrayList<>();
        for (BuildingDefinition definition : buildingDataManager.rawDefinitions()) {
            List<TraitId> unknownTraits = definition.traitAffinities().keySet().stream()
                    .filter(traitId -> !knownTraits.contains(traitId))
                    .toList();
            if (unknownTraits.isEmpty()) {
                validBuildings.add(definition);
            } else {
                log.error("Dropping building '{}': it references unknown traits {}", definition.id(), unknownTraits);
            }
        }

        scorerDataManager.replaceActiveScorers(validScorers);
        buildingDataManager.replaceActiveDefinitions(validBuildings);
        log.debug("Validated generation data against {} known traits: {} scorers active, {} buildings active",
                knownTraits.size(), validScorers.size(), validBuildings.size());
    }

}
