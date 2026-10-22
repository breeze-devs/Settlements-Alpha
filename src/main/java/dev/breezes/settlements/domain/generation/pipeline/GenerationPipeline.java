package dev.breezes.settlements.domain.generation.pipeline;

import dev.breezes.settlements.domain.common.BiomeId;
import dev.breezes.settlements.domain.generation.building.BuildingManifestCalculator;
import dev.breezes.settlements.domain.generation.building.BuildingRegistry;
import dev.breezes.settlements.domain.generation.history.HistoryEventEngine;
import dev.breezes.settlements.domain.generation.history.HistoryEventRegistry;
import dev.breezes.settlements.domain.generation.history.HistoryEventResult;
import dev.breezes.settlements.domain.generation.history.SettlementFoundingKind;
import dev.breezes.settlements.domain.generation.layout.SettlementLayoutEngine;
import dev.breezes.settlements.domain.generation.model.GenerationResult;
import dev.breezes.settlements.domain.generation.model.SettlementCharacter;
import dev.breezes.settlements.domain.generation.model.profile.ScaleTier;
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

    /**
     * Generates an established settlement on its site.
     *
     * @return the settlement, or empty anything in the pipeline went wrong
     */
    public Optional<GenerationResult> generate(@Nonnull TerrainGrid terrainGrid,
                                               @Nonnull SurveyBounds bounds,
                                               long seed) {
        Random random = new Random(seed);
        ScaleTier scaleTier = new SamplingEngine().determineScaleTier(random);
        int initialPopulation = random.nextInt(scaleTier.minPopulation(), scaleTier.maxPopulation() + 1);
        // Its own seed rather than this one, so the character's draws do not replay the tier and population rolls.
        // Generated settlements are established: their structure stands before any record of them begins.
        SettlementCharacter character = this.character(terrainGrid, bounds, scaleTier, initialPopulation,
                SettlementFoundingKind.ESTABLISHED, random.nextLong());

        BuildingManifestCalculator manifestCalculator = new BuildingManifestCalculator(this.buildingRegistry);
        SettlementLayoutEngine layoutEngine = new SettlementLayoutEngine();
        return manifestCalculator.calculate(character.profile(), random)
                .flatMap(manifest -> layoutEngine.generateLayout(character.siteReport(), character.profile(), manifest, this.biomeLookup))
                .map(layout -> new GenerationResult(character.siteReport(), character.profile(), character.history(), layout, seed));
    }

    /**
     * Decides a settlement's character from its site: surveys the terrain, scores the traits, samples a profile, and
     * draws the settlement's history.
     *
     * @param scaleTier         the settlement's tier, which the profile keeps
     * @param initialPopulation the settlement's initial population, which the profile keeps
     * @param founding          whether the settlement is established or new, which decides the history it may draw
     * @param seed              seeds every random draw the character makes
     */
    public SettlementCharacter character(@Nonnull TerrainGrid terrainGrid,
                                         @Nonnull SurveyBounds bounds,
                                         @Nonnull ScaleTier scaleTier,
                                         int initialPopulation,
                                         @Nonnull SettlementFoundingKind founding,
                                         long seed) {
        SurveyEngine surveyEngine = new SurveyEngine(this.biomeLookup);
        SiteReport report = surveyEngine.analyze(terrainGrid, bounds);

        ScoreEngine scoreEngine = new ScoreEngine(this.traitScorerRegistry);
        Map<TraitId, Float> scores = scoreEngine.score(report);

        Random random = new Random(seed);
        SamplingEngine samplingEngine = new SamplingEngine();
        SettlementProfile sampled = samplingEngine.sample(scores, scaleTier, initialPopulation, random);

        HistoryEventEngine historyEngine = new HistoryEventEngine(this.historyEventRegistry, this.traitRegistry);
        HistoryEventResult history = historyEngine.roll(sampled, report, founding, random);

        SettlementProfile profile = sampled.toBuilder()
                .adjustedWeights(history.modifiedWeights())
                .build();
        return new SettlementCharacter(report, profile, history);
    }

    /**
     * Derives template selection tags for the dominant biome in a site report.
     */
    public Set<String> resolveTemplateTags(@Nonnull SiteReport siteReport) {
        Optional<BiomeId> dominantBiome = siteReport.biomeDistribution().entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);

        return dominantBiome.map(biomeId -> this.biomeLookup.lookup(biomeId).templateTags())
                .orElseGet(Set::of);
    }

}
