package dev.breezes.settlements.infrastructure.minecraft.data.building;

import dev.breezes.settlements.domain.generation.building.BuildingRegistry;
import dev.breezes.settlements.domain.generation.model.building.BuildingDefinition;
import dev.breezes.settlements.domain.generation.model.building.BuildingDefinitionCodec;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.infrastructure.minecraft.data.framework.CodecJsonDataManager;
import jakarta.inject.Inject;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BuildingDefinitionDataManager extends CodecJsonDataManager<BuildingDefinition> implements BuildingRegistry {

    private static final String DIRECTORY_PATH = "settlements/buildings/definitions";

    private List<BuildingDefinition> rawDefinitions = List.of();
    private List<BuildingDefinition> definitions = List.of();
    private List<BuildingDefinition> constrained = List.of();
    private List<BuildingDefinition> unconstrained = List.of();
    private Map<TraitId, List<BuildingDefinition>> forTraitMap = Map.of();
    private Map<String, BuildingDefinition> byId = Map.of();

    @Inject
    public BuildingDefinitionDataManager() {
        super(DIRECTORY_PATH, BuildingDefinitionCodec.CODEC);
    }

    @Override
    protected String label() {
        return "building definition";
    }

    @Override
    protected void onReloaded(@Nonnull Map<ResourceLocation, BuildingDefinition> values) {
        // A later file overrides an earlier one that declares the same building id
        Map<String, BuildingDefinition> deduplicatedById = new LinkedHashMap<>();
        for (BuildingDefinition definition : values.values()) {
            deduplicatedById.put(definition.id(), definition);
        }

        List<BuildingDefinition> immutableDefinitions = List.copyOf(deduplicatedById.values());
        this.rawDefinitions = immutableDefinitions;
        this.replaceActiveDefinitions(immutableDefinitions);
    }

    public List<BuildingDefinition> rawDefinitions() {
        return this.rawDefinitions;
    }

    public void replaceActiveDefinitions(@Nonnull List<BuildingDefinition> validatedDefinitions) {
        List<BuildingDefinition> immutableDefinitions = List.copyOf(validatedDefinitions);
        Map<String, BuildingDefinition> parsedById = new HashMap<>();
        List<BuildingDefinition> parsedConstrained = new ArrayList<>();
        List<BuildingDefinition> parsedUnconstrained = new ArrayList<>();
        Map<TraitId, List<BuildingDefinition>> parsedForTrait = new LinkedHashMap<>();

        for (BuildingDefinition definition : immutableDefinitions) {
            parsedById.put(definition.id(), definition);
            if (definition.requiresResources().isEmpty()) {
                parsedUnconstrained.add(definition);
            } else {
                parsedConstrained.add(definition);
            }
            for (TraitId trait : definition.traitAffinities().keySet()) {
                parsedForTrait.computeIfAbsent(trait, ignored -> new ArrayList<>()).add(definition);
            }
        }

        for (Map.Entry<TraitId, List<BuildingDefinition>> entry : parsedForTrait.entrySet()) {
            TraitId trait = entry.getKey();
            entry.setValue(entry.getValue().stream()
                    .sorted(Comparator.comparingDouble((BuildingDefinition definition) -> definition.traitAffinities().get(trait)).reversed())
                    .toList());
        }

        this.definitions = immutableDefinitions;
        this.constrained = List.copyOf(parsedConstrained);
        this.unconstrained = List.copyOf(parsedUnconstrained);
        this.forTraitMap = Map.copyOf(parsedForTrait);
        this.byId = Map.copyOf(parsedById);
    }

    @Override
    public List<BuildingDefinition> allBuildings() {
        return this.definitions;
    }

    @Override
    public List<BuildingDefinition> constrainedBuildings() {
        return this.constrained;
    }

    @Override
    public List<BuildingDefinition> unconstrainedBuildings() {
        return this.unconstrained;
    }

    @Override
    public List<BuildingDefinition> forTrait(TraitId trait) {
        return this.forTraitMap.getOrDefault(trait, List.of());
    }

    @Override
    public Optional<BuildingDefinition> byId(String id) {
        return Optional.ofNullable(this.byId.get(id));
    }

}
