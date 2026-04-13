package dev.breezes.settlements.infrastructure.minecraft.data.scoring;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import dev.breezes.settlements.domain.common.BiomeId;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.domain.generation.model.survey.ResourceTag;
import dev.breezes.settlements.domain.generation.model.survey.WaterFeatureType;
import dev.breezes.settlements.domain.generation.scoring.ConfiguredTraitScorer;
import dev.breezes.settlements.domain.generation.scoring.TraitScorer;
import dev.breezes.settlements.domain.generation.scoring.TraitScorerConfig;
import dev.breezes.settlements.domain.generation.scoring.TraitScorerRegistry;
import lombok.CustomLog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CustomLog
public class TraitScorerDataManager extends SimpleJsonResourceReloadListener implements TraitScorerRegistry {

    private static final String DIRECTORY_PATH = "settlements/traits/scoring";

    private static final Gson GSON = new GsonBuilder().create();

    private Map<TraitId, TraitScorer> rawScorersByTrait = Map.of();
    private Map<TraitId, TraitScorer> activeScorersByTrait = Map.of();

    @Inject
    public TraitScorerDataManager() {
        super(GSON, DIRECTORY_PATH);
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> entries,
                         @Nonnull ResourceManager resourceManager,
                         @Nonnull ProfilerFiller profiler) {
        Map<TraitId, TraitScorer> parsed = new LinkedHashMap<>();
        int errorCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .toList()) {
            ResourceLocation fileId = entry.getKey();
            JsonElement json = entry.getValue();

            try {
                TraitScorerEntry scorerEntry = GSON.fromJson(json, TraitScorerEntry.class);
                TraitScorerConfig config = toConfig(scorerEntry, fileId.toString());
                if (parsed.containsKey(config.trait())) {
                    log.error("Duplicate trait scorer entry for '{}' from file '{}', overwriting", config.trait(), fileId);
                }
                parsed.put(config.trait(), new ConfiguredTraitScorer(config));
            } catch (Exception e) {
                log.warn("Failed to parse trait scorer data from file '{}': {}", fileId, e.getMessage());
                errorCount++;
            }
        }

        Map<TraitId, TraitScorer> immutable = Map.copyOf(parsed);
        this.rawScorersByTrait = immutable;
        this.activeScorersByTrait = immutable;
        log.info("Loaded {} trait scorer configs ({} errors)", parsed.size(), errorCount);
    }

    public void loadForTest(@Nonnull Map<ResourceLocation, JsonElement> entries) {
        this.apply(entries, null, null);
    }

    @Override
    public Map<TraitId, TraitScorer> allScorers() {
        return this.activeScorersByTrait;
    }

    public Map<TraitId, TraitScorer> rawScorers() {
        return this.rawScorersByTrait;
    }

    public void replaceActiveScorers(@Nonnull Map<TraitId, TraitScorer> validatedScorers) {
        this.activeScorersByTrait = Map.copyOf(validatedScorers);
    }

    private static TraitScorerConfig toConfig(@Nonnull TraitScorerEntry entry, @Nonnull String fileId) {
        if (entry.getTrait() == null || entry.getTrait().isBlank()) {
            throw new IllegalArgumentException("missing trait");
        }

        return new TraitScorerConfig(
                TraitId.of(entry.getTrait()),
                entry.getBaseScore() == null ? 0.0f : entry.getBaseScore(),
                parseEnumFloatMap(entry.getResourceTagWeights(), ResourceTag.class, fileId, "resource tag"),
                parseEnumSet(entry.getRequiredTags(), ResourceTag.class, fileId, "required tag"),
                parseEnumSet(entry.getVetoTags(), ResourceTag.class, fileId, "veto tag"),
                parseEnumFloatMap(entry.getWaterFeatureWeights(), WaterFeatureType.class, fileId, "water feature"),
                parseBiomeWeights(entry.getBiomeWeights(), fileId),
                entry.getElevationDeltaWeight() == null ? 0.0f : entry.getElevationDeltaWeight(),
                entry.getElevationDeltaNormalization() == null ? 0.0f : entry.getElevationDeltaNormalization()
        );
    }

    private static Map<BiomeId, Float> parseBiomeWeights(Map<String, Float> raw, String fileId) {
        Map<BiomeId, Float> parsed = new HashMap<>();
        if (raw == null) {
            return parsed;
        }

        for (Map.Entry<String, Float> entry : raw.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                log.warn("Ignoring blank biome key in '{}'", fileId);
                continue;
            }
            if (entry.getValue() == null) {
                log.warn("Ignoring null biome weight '{}' in '{}'", entry.getKey(), fileId);
                continue;
            }
            parsed.put(BiomeId.of(entry.getKey()), entry.getValue());
        }

        return parsed;
    }

    private static <E extends Enum<E>> Map<E, Float> parseEnumFloatMap(Map<String, Float> raw,
                                                                       Class<E> enumType,
                                                                       String fileId,
                                                                       String fieldLabel) {
        Map<E, Float> parsed = new EnumMap<>(enumType);
        if (raw == null) {
            return parsed;
        }

        for (Map.Entry<String, Float> entry : raw.entrySet()) {
            if (entry.getKey() == null) {
                log.warn("Ignoring null {} key in '{}'", fieldLabel, fileId);
                continue;
            }
            if (entry.getValue() == null) {
                log.warn("Ignoring null {} weight '{}' in '{}'", fieldLabel, entry.getKey(), fileId);
                continue;
            }
            try {
                parsed.put(Enum.valueOf(enumType, entry.getKey()), entry.getValue());
            } catch (IllegalArgumentException ex) {
                log.warn("Ignoring invalid {} '{}' in '{}'", fieldLabel, entry.getKey(), fileId);
            }
        }

        return parsed;
    }

    private static <E extends Enum<E>> Set<E> parseEnumSet(List<String> raw,
                                                           Class<E> enumType,
                                                           String fileId,
                                                           String fieldLabel) {
        Set<E> parsed = EnumSet.noneOf(enumType);
        if (raw == null) {
            return parsed;
        }

        for (String value : raw) {
            if (value == null) {
                log.warn("Ignoring null {} in '{}'", fieldLabel, fileId);
                continue;
            }
            try {
                parsed.add(Enum.valueOf(enumType, value));
            } catch (IllegalArgumentException ex) {
                log.warn("Ignoring invalid {} '{}' in '{}'", fieldLabel, value, fileId);
            }
        }

        return parsed;
    }

}
