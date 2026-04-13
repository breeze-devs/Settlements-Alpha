package dev.breezes.settlements.infrastructure.minecraft.data.building;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import dev.breezes.settlements.domain.generation.building.BuildingRegistry;
import dev.breezes.settlements.domain.generation.model.IntRange;
import dev.breezes.settlements.domain.generation.model.building.BuildingDefinition;
import dev.breezes.settlements.domain.generation.model.building.FootprintConstraint;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.domain.generation.model.profile.TraitSlot;
import dev.breezes.settlements.domain.generation.model.survey.ResourceTag;
import lombok.CustomLog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@CustomLog
public class BuildingDefinitionDataManager extends SimpleJsonResourceReloadListener implements BuildingRegistry {

    private static final String DIRECTORY_PATH = "settlements/buildings/definitions";

    private static final Gson GSON = new GsonBuilder().create();

    private List<BuildingDefinition> rawDefinitions = List.of();
    private List<BuildingDefinition> definitions = List.of();
    private List<BuildingDefinition> constrained = List.of();
    private List<BuildingDefinition> unconstrained = List.of();
    private Map<TraitId, List<BuildingDefinition>> forTraitMap = Map.of();
    private Map<String, BuildingDefinition> byId = Map.of();

    @Inject
    public BuildingDefinitionDataManager() {
        super(GSON, DIRECTORY_PATH);
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> entries,
                         @Nonnull ResourceManager resourceManager,
                         @Nonnull ProfilerFiller profiler) {
        List<BuildingDefinition> parsed = new ArrayList<>();
        int errorCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            JsonElement json = entry.getValue();

            try {
                BuildingDefinitionEntry definitionEntry = GSON.fromJson(json, BuildingDefinitionEntry.class);
                if (definitionEntry == null) {
                    log.warn("Invalid building definition in '{}': parsed entry was null", fileId);
                    errorCount++;
                    continue;
                }
                parsed.add(toDefinition(definitionEntry, fileId.toString()));
            } catch (Exception e) {
                log.warn("Failed to parse building definition from file '{}': {}", fileId, e.getMessage());
                errorCount++;
            }
        }

        Map<String, BuildingDefinition> deduplicatedById = new LinkedHashMap<>();
        for (BuildingDefinition definition : parsed) {
            BuildingDefinition previous = deduplicatedById.put(definition.id(), definition);
            if (previous != null) {
                log.error("Duplicate building definition id '{}', keeping later entry", definition.id());
            }
        }

        List<BuildingDefinition> immutableDefinitions = List.copyOf(deduplicatedById.values());
        this.rawDefinitions = immutableDefinitions;
        this.replaceActiveDefinitions(immutableDefinitions);

        log.info("Loaded {} building definitions ({} errors)", this.definitions.size(), errorCount);
    }

    public void loadForTest(@Nonnull Map<ResourceLocation, JsonElement> entries) {
        this.apply(entries, null, null);
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

    private static BuildingDefinition toDefinition(@Nonnull BuildingDefinitionEntry entry, @Nonnull String fileId) {
        if (entry.getId() == null || entry.getId().isBlank()) {
            throw new IllegalArgumentException("missing id");
        }
        if (entry.getPlacementPriority() == null) {
            throw new IllegalArgumentException("missing placement_priority");
        }
        if (entry.getZoneTierMin() == null || entry.getZoneTierMax() == null) {
            throw new IllegalArgumentException("missing zone tier bounds");
        }
        if (entry.getZoneTierMin() < 0 || entry.getZoneTierMax() > 4) {
            throw new IllegalArgumentException("zone tier bounds must be within 0-4");
        }
        if (entry.getZoneTierMin() > entry.getZoneTierMax()) {
            throw new IllegalArgumentException("zone_tier_min must be <= zone_tier_max");
        }
        if (entry.getFootprintMinWidth() == null || entry.getFootprintMaxWidth() == null
                || entry.getFootprintMinDepth() == null || entry.getFootprintMaxDepth() == null) {
            throw new IllegalArgumentException("missing footprint bounds");
        }
        if (entry.getFootprintMinWidth() > entry.getFootprintMaxWidth()
                || entry.getFootprintMinDepth() > entry.getFootprintMaxDepth()) {
            throw new IllegalArgumentException("footprint min dimensions must be <= max dimensions");
        }

        return BuildingDefinition.builder()
                .id(entry.getId())
                .displayInfo(null)
                .traitAffinities(parseTraitAffinities(entry.getTraitAffinities(), fileId))
                .minimumRank(parseMinimumRank(entry.getMinimumRank(), fileId))
                .placementPriority(entry.getPlacementPriority())
                .zoneTierPreference(IntRange.of(entry.getZoneTierMin(), entry.getZoneTierMax()))
                .requiresRoadFrontage(Boolean.TRUE.equals(entry.getRequiresRoadFrontage()))
                .requiresResources(parseResourceTags(entry.getRequiresResources(), fileId, "required resource"))
                .forbiddenResources(parseResourceTags(entry.getForbiddenResources(), fileId, "forbidden resource"))
                .footprint(FootprintConstraint.builder()
                        .minWidth(entry.getFootprintMinWidth())
                        .maxWidth(entry.getFootprintMaxWidth())
                        .minDepth(entry.getFootprintMinDepth())
                        .maxDepth(entry.getFootprintMaxDepth())
                        .build())
                .preferredTags(parsePreferredTags(entry.getPreferredTags(), fileId))
                .proximityAffinities(List.of())
                .globalAffinities(List.of())
                .npcProfession(entry.getNpcProfession())
                .npcCount(entry.getNpcCount() == null ? 0 : entry.getNpcCount())
                .build();
    }

    private static Map<TraitId, Float> parseTraitAffinities(Map<String, Float> raw, String fileId) {
        Map<TraitId, Float> parsed = new LinkedHashMap<>();
        if (raw == null) {
            return parsed;
        }

        for (Map.Entry<String, Float> entry : raw.entrySet()) {
            if (entry.getKey() == null) {
                log.warn("Ignoring null trait affinity key in '{}'", fileId);
                continue;
            }
            if (entry.getValue() == null) {
                log.warn("Ignoring null trait affinity value '{}' in '{}'", entry.getKey(), fileId);
                continue;
            }
            try {
                parsed.put(TraitId.of(entry.getKey()), entry.getValue());
            } catch (IllegalArgumentException ex) {
                log.warn("Ignoring invalid trait affinity '{}' in '{}'", entry.getKey(), fileId);
            }
        }

        return parsed;
    }

    private static Set<ResourceTag> parseResourceTags(List<String> raw, String fileId, String fieldLabel) {
        Set<ResourceTag> parsed = EnumSet.noneOf(ResourceTag.class);
        if (raw == null) {
            return parsed;
        }

        for (String value : raw) {
            if (value == null) {
                log.warn("Ignoring null {} in '{}'", fieldLabel, fileId);
                continue;
            }
            try {
                parsed.add(ResourceTag.valueOf(value));
            } catch (IllegalArgumentException ex) {
                log.warn("Ignoring invalid {} '{}' in '{}'", fieldLabel, value, fileId);
            }
        }

        return parsed;
    }

    private static Set<String> parsePreferredTags(@Nullable List<String> raw, String fileId) {
        if (raw == null) {
            return Set.of();
        }

        Set<String> parsed = new LinkedHashSet<>();
        for (String value : raw) {
            if (value == null || value.isBlank()) {
                log.warn("Ignoring blank preferred tag in '{}'", fileId);
                continue;
            }
            parsed.add(value);
        }
        return Set.copyOf(parsed);
    }

    private static TraitSlot parseMinimumRank(String raw, String fileId) {
        if (raw == null || raw.isBlank()) {
            return TraitSlot.FLAVOR;
        }

        try {
            return TraitSlot.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid minimum rank '{}' in '{}', defaulting to FLAVOR", raw, fileId);
            return TraitSlot.FLAVOR;
        }
    }

}
