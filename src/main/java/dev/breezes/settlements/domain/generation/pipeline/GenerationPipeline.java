package dev.breezes.settlements.domain.generation.pipeline;

import dev.breezes.settlements.domain.common.BiomeId;
import dev.breezes.settlements.domain.generation.building.BuildingManifestCalculator;
import dev.breezes.settlements.domain.generation.building.BuildingRegistry;
import dev.breezes.settlements.domain.generation.history.HistoryEventEngine;
import dev.breezes.settlements.domain.generation.history.HistoryEventRegistry;
import dev.breezes.settlements.domain.generation.history.HistoryEventResult;
import dev.breezes.settlements.domain.generation.layout.LayoutResult;
import dev.breezes.settlements.domain.generation.layout.SettlementLayoutEngine;
import dev.breezes.settlements.domain.generation.model.GenerationResult;
import dev.breezes.settlements.domain.generation.model.building.BuildingManifest;
import dev.breezes.settlements.domain.generation.model.profile.SettlementProfile;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.domain.generation.model.survey.SiteReport;
import dev.breezes.settlements.domain.generation.model.survey.SurveyBounds;
import dev.breezes.settlements.domain.generation.model.survey.TerrainGrid;
import dev.breezes.settlements.domain.generation.sampling.SamplingEngine;
import dev.breezes.settlements.domain.generation.scoring.ScoreEngine;
import dev.breezes.settlements.domain.generation.scoring.TraitScorerRegistry;
import dev.breezes.settlements.domain.generation.survey.BiomeSurveyLookup;
import dev.breezes.settlements.domain.generation.survey.SurveyEngine;
import dev.breezes.settlements.domain.generation.trait.TraitRegistry;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

@AllArgsConstructor
public class GenerationPipeline {

    private final BiomeSurveyLookup biomeLookup;
    private final TraitScorerRegistry traitScorerRegistry;
    private final BuildingRegistry buildingRegistry;
    private final HistoryEventRegistry historyEventRegistry;
    private final TraitRegistry traitRegistry;

    public GenerationResult generate(@Nonnull TerrainGrid terrainGrid,
                                     @Nonnull SurveyBounds bounds,
                                     long seed) {
        SurveyEngine surveyEngine = new SurveyEngine(this.biomeLookup);
        SiteReport report = surveyEngine.analyze(terrainGrid, bounds);

        ScoreEngine scoreEngine = new ScoreEngine(this.traitScorerRegistry);
        Map<TraitId, Float> scores = scoreEngine.score(report);

        Random random = new Random(seed);
        SamplingEngine samplingEngine = new SamplingEngine();
        SettlementProfile profile = samplingEngine.sample(scores, random);

        HistoryEventEngine historyEngine = new HistoryEventEngine(this.historyEventRegistry, this.traitRegistry);
        HistoryEventResult historyResult = historyEngine.roll(profile, report, random);

        profile = SettlementProfile.builder()
                .primary(profile.primary())
                .secondary(profile.secondary())
                .flavor(profile.flavor())
                .adjustedWeights(historyResult.modifiedWeights())
                .scaleTier(profile.scaleTier())
                .estimatedPopulation(profile.estimatedPopulation())
                .wealthLevel(profile.wealthLevel())
                .defenseLevel(profile.defenseLevel())
                .seed(profile.seed())
                .historyEventIds(historyResult.eventIds())
                .build();

        BuildingManifestCalculator manifestCalculator = new BuildingManifestCalculator(this.buildingRegistry);
        BuildingManifest manifest = manifestCalculator.calculate(profile, random);

        SettlementLayoutEngine layoutEngine = new SettlementLayoutEngine();
        LayoutResult layout = layoutEngine.generateLayout(report, profile, manifest, this.biomeLookup);

        return new GenerationResult(report, profile, historyResult, layout, seed);
    }

    /**
     * Derives template selection tags for the dominant biome in a site report.
     */
    public Set<String> resolveTemplateTags(@Nonnull SiteReport siteReport) {
        Optional<BiomeId> dominantBiome = siteReport.biomeDistribution().entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);

        if (dominantBiome.isEmpty()) {
            return Set.of();
        }

        return this.biomeLookup.lookup(dominantBiome.get()).templateTags();
    }

}
