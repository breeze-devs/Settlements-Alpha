package dev.breezes.settlements.infrastructure.minecraft.data.survey;

import dev.breezes.settlements.domain.common.BiomeId;
import dev.breezes.settlements.domain.generation.survey.BiomeSurveyData;
import dev.breezes.settlements.domain.generation.survey.BiomeSurveyDefinition;
import dev.breezes.settlements.domain.generation.survey.BiomeSurveyDefinitionCodec;
import dev.breezes.settlements.domain.generation.survey.BiomeSurveyLookup;
import dev.breezes.settlements.infrastructure.minecraft.data.framework.KeyedCatalogDataManager;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;

public class BiomeSurveyDataManager extends KeyedCatalogDataManager<BiomeId, BiomeSurveyDefinition> implements BiomeSurveyLookup {

    private static final String DIRECTORY_PATH = "settlements/biomes/survey";

    @Inject
    public BiomeSurveyDataManager() {
        super(DIRECTORY_PATH, BiomeSurveyDefinitionCodec.CODEC);
    }

    @Override
    protected String label() {
        return "biome survey";
    }

    @Override
    protected BiomeId keyOf(@Nonnull BiomeSurveyDefinition value) {
        return value.biome();
    }

    /**
     * Retrieves the survey data given a biome ID, or the default data if not found
     */
    @Override
    public BiomeSurveyData lookup(@Nonnull BiomeId biome) {
        return this.find(biome).map(BiomeSurveyDefinition::survey).orElse(BiomeSurveyData.DEFAULT);
    }

}
