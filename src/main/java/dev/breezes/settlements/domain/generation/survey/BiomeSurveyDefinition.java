package dev.breezes.settlements.domain.generation.survey;

import dev.breezes.settlements.domain.common.BiomeId;

import javax.annotation.Nonnull;

/**
 * A single decoded {@code biomes/survey} datapack entry: the biome the survey data applies to,
 * plus the survey data itself. {@link BiomeSurveyData} stays biome-agnostic (it is also used as
 * the DEFAULT fallback) so the biome key lives in this wrapper instead.
 */
public record BiomeSurveyDefinition(
        @Nonnull BiomeId biome,
        @Nonnull BiomeSurveyData survey
) {
}
