package dev.breezes.settlements.infrastructure.minecraft.data.history;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import dev.breezes.settlements.domain.generation.history.EventPreconditions;
import dev.breezes.settlements.domain.generation.history.HistoryEventDefinition;
import dev.breezes.settlements.domain.generation.history.HistoryEventRegistry;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.domain.generation.model.survey.ResourceTag;
import dev.breezes.settlements.domain.generation.model.survey.WaterFeatureType;
import lombok.CustomLog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CustomLog
public class HistoryEventDataManager extends SimpleJsonResourceReloadListener implements HistoryEventRegistry {

    private static final String DIRECTORY_PATH = "settlements/history/events";
    private static final Gson GSON = new GsonBuilder().create();

    private List<HistoryEventDefinition> definitions = List.of();

    @Inject
    public HistoryEventDataManager() {
        super(GSON, DIRECTORY_PATH);
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> entries,
                         @Nonnull ResourceManager resourceManager,
                         @Nonnull ProfilerFiller profiler) {
        Map<String, HistoryEventDefinition> parsedById = new LinkedHashMap<>();
        int errorCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .toList()) {
            ResourceLocation fileId = entry.getKey();
            try {
                HistoryEventEntry historyEventEntry = GSON.fromJson(entry.getValue(), HistoryEventEntry.class);
                HistoryEventDefinition definition = toDefinition(historyEventEntry, fileId.toString());
                HistoryEventDefinition previous = parsedById.put(definition.id(), definition);
                if (previous != null) {
                    log.error("Duplicate history event id '{}', keeping later entry", definition.id());
                }
            } catch (Exception e) {
                log.warn("Failed to parse history event from file '{}': {}", fileId, e.getMessage());
                errorCount++;
            }
        }

        this.definitions = List.copyOf(parsedById.values());
        log.info("Loaded {} history events ({} errors)", this.definitions.size(), errorCount);
    }

    public void loadForTest(@Nonnull Map<ResourceLocation, JsonElement> entries) {
        this.apply(entries, null, null);
    }

    @Override
    public List<HistoryEventDefinition> allEvents() {
        return this.definitions;
    }

    private static HistoryEventDefinition toDefinition(HistoryEventEntry entry, String fileId) {
        if (entry == null) {
            throw new IllegalArgumentException("parsed entry was null");
        }
        if (entry.getId() == null || entry.getId().isBlank()) {
            throw new IllegalArgumentException("missing id");
        }
        if (entry.getCategory() == null || entry.getCategory().isBlank()) {
            throw new IllegalArgumentException("missing category");
        }
        if (entry.getTimeHorizonMin() == null || entry.getTimeHorizonMax() == null) {
            throw new IllegalArgumentException("missing time horizon bounds");
        }
        if (entry.getTimeHorizonMax() < entry.getTimeHorizonMin()) {
            throw new IllegalArgumentException("time_horizon_max must be >= time_horizon_min");
        }
        if (entry.getNarrativeText() == null || entry.getNarrativeText().isBlank()) {
            throw new IllegalArgumentException("missing narrative_text");
        }

        float probabilityWeight = entry.getProbabilityWeight() == null ? 1.0f : entry.getProbabilityWeight();
        if (probabilityWeight <= 0.0f) {
            throw new IllegalArgumentException("probability_weight must be > 0");
        }

        return HistoryEventDefinition.builder()
                .id(entry.getId())
                .category(entry.getCategory())
                .timeHorizonMin(entry.getTimeHorizonMin())
                .timeHorizonMax(entry.getTimeHorizonMax())
                .exclusiveTags(parseStringList(entry.getExclusiveTags(), fileId, "exclusive tag"))
                .probabilityWeight(probabilityWeight)
                .preconditions(parsePreconditions(entry.getPreconditions(), fileId))
                .traitModifiers(parseTraitMap(entry.getTraitModifiers(), fileId, "trait modifier"))
                .visualMarkers(parseStringList(entry.getVisualMarkers(), fileId, "visual marker"))
                .narrativeText(entry.getNarrativeText())
                .build();
    }

    private static EventPreconditions parsePreconditions(PreconditionsEntry entry, String fileId) {
        if (entry == null) {
            return EventPreconditions.NONE;
        }

        return EventPreconditions.builder()
                .minTraitWeights(parseTraitMap(entry.getMinTraitWeights(), fileId, "minimum trait weight"))
                .requiredResourceTags(parseResourceTags(entry.getRequiredResourceTags(), fileId, "required resource tag"))
                .requiredWaterFeatures(parseWaterFeatures(entry.getRequiredWaterFeatures(), fileId, "required water feature"))
                .minPopulation(entry.getMinPopulation() == null ? 0 : entry.getMinPopulation())
                .build();
    }

    private static Map<TraitId, Float> parseTraitMap(Map<String, Float> raw, String fileId, String fieldLabel) {
        Map<TraitId, Float> parsed = new LinkedHashMap<>();
        if (raw == null) {
            return parsed;
        }

        for (Map.Entry<String, Float> entry : raw.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                log.warn("Ignoring blank {} key in '{}'", fieldLabel, fileId);
                continue;
            }
            if (!entry.getKey().contains(":")) {
                log.warn("Ignoring non-namespaced {} '{}' in '{}'", fieldLabel, entry.getKey(), fileId);
                continue;
            }
            if (entry.getValue() == null) {
                log.warn("Ignoring null {} value '{}' in '{}'", fieldLabel, entry.getKey(), fileId);
                continue;
            }
            try {
                parsed.put(TraitId.of(entry.getKey()), entry.getValue());
            } catch (IllegalArgumentException ex) {
                log.warn("Ignoring invalid {} '{}' in '{}'", fieldLabel, entry.getKey(), fileId);
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

    private static Set<WaterFeatureType> parseWaterFeatures(List<String> raw, String fileId, String fieldLabel) {
        Set<WaterFeatureType> parsed = EnumSet.noneOf(WaterFeatureType.class);
        if (raw == null) {
            return parsed;
        }

        for (String value : raw) {
            if (value == null) {
                log.warn("Ignoring null {} in '{}'", fieldLabel, fileId);
                continue;
            }
            try {
                parsed.add(WaterFeatureType.valueOf(value));
            } catch (IllegalArgumentException ex) {
                log.warn("Ignoring invalid {} '{}' in '{}'", fieldLabel, value, fileId);
            }
        }

        return parsed;
    }

    private static List<String> parseStringList(List<String> raw, String fileId, String fieldLabel) {
        if (raw == null) {
            return List.of();
        }

        List<String> parsed = new ArrayList<>();
        for (String value : raw) {
            if (value == null || value.isBlank()) {
                log.warn("Ignoring blank {} in '{}'", fieldLabel, fileId);
                continue;
            }
            parsed.add(value);
        }
        return List.copyOf(parsed);
    }

}
