package dev.breezes.settlements.infrastructure.minecraft.data.survey;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import dev.breezes.settlements.domain.common.BiomeId;
import dev.breezes.settlements.domain.generation.model.survey.ResourceTag;
import dev.breezes.settlements.domain.generation.model.survey.WaterFeatureType;
import dev.breezes.settlements.domain.generation.survey.BiomeSurveyData;
import dev.breezes.settlements.domain.generation.survey.BiomeSurveyLookup;
import lombok.CustomLog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CustomLog
public class BiomeSurveyDataManager extends SimpleJsonResourceReloadListener implements BiomeSurveyLookup {

    private static final String DIRECTORY_PATH = "settlements/biomes/survey";

    private static final Gson GSON = new GsonBuilder().create();

    private Map<BiomeId, BiomeSurveyData> dataByBiome = Map.of();

    @Inject
    public BiomeSurveyDataManager() {
        super(GSON, DIRECTORY_PATH);
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> entries,
                         @Nonnull ResourceManager resourceManager,
                         @Nonnull ProfilerFiller profiler) {
        Map<BiomeId, BiomeSurveyData> parsed = new HashMap<>();
        int errorCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            JsonElement json = entry.getValue();

            try {
                BiomeSurveyEntry surveyEntry = GSON.fromJson(json, BiomeSurveyEntry.class);
                if (surveyEntry == null || surveyEntry.getBiome() == null || surveyEntry.getBiome().isBlank()) {
                    log.warn("Invalid biome survey entry in '{}': missing biome", fileId);
                    errorCount++;
                    continue;
                }

                BiomeId biomeId = BiomeId.of(surveyEntry.getBiome());
                BiomeSurveyData surveyData = toSurveyData(surveyEntry, fileId.toString());
                if (parsed.containsKey(biomeId)) {
                    log.error("Duplicate biome survey entry for '{}' from file '{}', overwriting", biomeId, fileId);
                }
                parsed.put(biomeId, surveyData);
            } catch (Exception e) {
                log.warn("Failed to parse biome survey data from file '{}': {}", fileId, e.getMessage());
                errorCount++;
            }
        }

        this.dataByBiome = Map.copyOf(parsed);
        log.info("Loaded {} biome survey entries ({} errors)", parsed.size(), errorCount);
    }

    /**
     * Retrieves the survey data given a biome ID, or the default data if not found
     */
    @Override
    public BiomeSurveyData lookup(@Nonnull BiomeId biome) {
        return this.dataByBiome.getOrDefault(biome, BiomeSurveyData.DEFAULT);
    }

    private static BiomeSurveyData toSurveyData(@Nonnull BiomeSurveyEntry entry, @Nonnull String fileId) {
        Map<ResourceTag, Float> resourceDensities = new EnumMap<>(ResourceTag.class);
        if (entry.getResourceDensities() != null) {
            for (Map.Entry<String, Float> densityEntry : entry.getResourceDensities().entrySet()) {
                if (densityEntry.getKey() == null) {
                    log.warn("Ignoring null resource tag key in biome survey entry '{}'", fileId);
                    continue;
                }
                try {
                    ResourceTag tag = ResourceTag.valueOf(densityEntry.getKey());
                    Float value = densityEntry.getValue();
                    if (value == null) {
                        log.warn("Ignoring null density for resource tag '{}' in '{}'", densityEntry.getKey(), fileId);
                        continue;
                    }
                    resourceDensities.put(tag, value);
                } catch (IllegalArgumentException ex) {
                    log.warn("Ignoring invalid resource tag '{}' in '{}'", densityEntry.getKey(), fileId);
                }
            }
        }

        WaterFeatureType waterType = null;
        if (entry.getWaterType() != null) {
            try {
                waterType = WaterFeatureType.valueOf(entry.getWaterType());
            } catch (IllegalArgumentException ex) {
                log.warn("Ignoring invalid water type '{}' in '{}'", entry.getWaterType(), fileId);
            }
        }

        return new BiomeSurveyData(resourceDensities, waterType, parseTemplateTags(entry.getTemplateTags(), fileId));
    }

    private static Set<String> parseTemplateTags(@Nullable List<String> raw, String fileId) {
        if (raw == null) {
            return Set.of();
        }

        Set<String> parsed = new LinkedHashSet<>();
        for (String value : raw) {
            if (value == null || value.isBlank()) {
                log.warn("Ignoring blank template tag in '{}'", fileId);
                continue;
            }
            parsed.add(value);
        }
        return parsed;
    }

}
