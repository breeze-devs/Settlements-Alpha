package dev.breezes.settlements.domain.generation.scoring;

import dev.breezes.settlements.domain.common.BiomeId;
import dev.breezes.settlements.domain.generation.model.geometry.BlockPosition;
import dev.breezes.settlements.domain.generation.model.geometry.BoundingRegion;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.domain.generation.model.survey.ResourceTag;
import dev.breezes.settlements.domain.generation.model.survey.SiteReport;
import dev.breezes.settlements.domain.generation.model.survey.SurveyBounds;
import dev.breezes.settlements.domain.generation.model.survey.TerrainGrid;
import dev.breezes.settlements.domain.generation.model.survey.TerrainSample;
import dev.breezes.settlements.domain.generation.model.survey.WaterFeatureType;
import dev.breezes.settlements.domain.generation.survey.BiomeSurveyData;
import dev.breezes.settlements.domain.generation.survey.SurveyEngine;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreEngineTest {

    private static final TraitId LUMBER = TraitId.of("settlements:settlement_traits/lumber");
    private static final TraitId FARMING = TraitId.of("settlements:settlement_traits/farming");
    private static final TraitId FISHING = TraitId.of("settlements:settlement_traits/fishing");
    private static final TraitId MINING = TraitId.of("settlements:settlement_traits/mining");
    private static final TraitId DEFENSE = TraitId.of("settlements:settlement_traits/defense");

    private final SurveyEngine surveyEngine = new SurveyEngine(this::lookup);
    private final ScoreEngine scoreEngine = new ScoreEngine(() -> defaultScorers());

    @Test
    void flatPlains_farmingDominates() {
        SiteReport report = this.surveyEngine.analyze(gridFilled(50, 50, 64, BiomeId.of("minecraft:plains")), boundsForGrid(50, 50));

        Map<TraitId, Float> scores = this.scoreEngine.score(report);
        assertEquals(0.58f, scores.get(FARMING), 0.0001f);
        assertEquals(0.0f, scores.get(FISHING), 0.0001f);
        assertEquals(0.1f, scores.get(DEFENSE), 0.0001f);
    }

    @Test
    void riverCrossing_farmingBeatsFishing() {
        TerrainSample[][] samples = filledSamples(50, 50, 64, BiomeId.of("minecraft:plains"));
        for (int z = 0; z < 50; z++) {
            for (int x = 23; x <= 25; x++) {
                samples[z][x] = new TerrainSample(62, BiomeId.of("minecraft:river"), 0.5f);
            }
        }

        Map<TraitId, Float> scores = this.scoreEngine.score(this.surveyEngine.analyze(TerrainGrid.of(0, 0, 1, samples), boundsForGrid(50, 50)));
        assertEquals(0.312f, scores.get(FISHING), 0.0001f);
        assertTrue(scores.get(FARMING) > scores.get(FISHING));
    }

    @Test
    void coastal_fishingDominates() {
        TerrainSample[][] samples = new TerrainSample[50][50];
        for (int z = 0; z < 50; z++) {
            for (int x = 0; x < 50; x++) {
                samples[z][x] = x < 25
                        ? new TerrainSample(64, BiomeId.of("minecraft:plains"), 0.8f)
                        : new TerrainSample(45, BiomeId.of("minecraft:ocean"), 0.5f);
            }
        }

        Map<TraitId, Float> scores = this.scoreEngine.score(this.surveyEngine.analyze(TerrainGrid.of(0, 0, 1, samples), boundsForGrid(50, 50)));
        assertEquals(0.5f, scores.get(FISHING), 0.0001f);
        assertEquals(0.29f, scores.get(FARMING), 0.0001f);
    }

    @Test
    void denseForest_lumberDominatesAndFarmingIsZero() {
        Map<TraitId, Float> scores = this.scoreEngine.score(
                this.surveyEngine.analyze(gridFilled(50, 50, 68, BiomeId.of("minecraft:dark_forest")), boundsForGrid(50, 50))
        );

        assertEquals(0.72f, scores.get(LUMBER), 0.0001f);
        assertEquals(0.0f, scores.get(FARMING), 0.0001f);
    }

    @Test
    void mountainous_miningAndDefenseAreHigh() {
        TerrainSample[][] samples = new TerrainSample[50][50];
        for (int z = 0; z < 50; z++) {
            for (int x = 0; x < 50; x++) {
                samples[z][x] = new TerrainSample(60 + ((x + z) % 61), BiomeId.of("minecraft:stony_peaks"), 0.2f);
            }
        }

        Map<TraitId, Float> scores = this.scoreEngine.score(this.surveyEngine.analyze(TerrainGrid.of(0, 0, 1, samples), boundsForGrid(50, 50)));
        assertEquals(0.73f, scores.get(MINING), 0.0001f);
        assertEquals(0.52f, scores.get(DEFENSE), 0.0001f);
    }

    @Test
    void frozenPlains_vetoesFarming() {
        Map<TraitId, Float> scores = this.scoreEngine.score(
                this.surveyEngine.analyze(gridFilled(50, 50, 64, BiomeId.of("minecraft:snowy_plains")), boundsForGrid(50, 50))
        );

        assertEquals(0.0f, scores.get(FARMING), 0.0001f);
        assertEquals(0.1f, scores.get(DEFENSE), 0.0001f);
    }

    @Test
    void determinism_sameReportTwice_sameScores() {
        SiteReport report = this.surveyEngine.analyze(gridFilled(50, 50, 64, BiomeId.of("minecraft:plains")), boundsForGrid(50, 50));

        assertEquals(this.scoreEngine.score(report), this.scoreEngine.score(report));
    }

    @Test
    void emptyRegistry_returnsEmptyMap() {
        ScoreEngine engine = new ScoreEngine(Map::of);
        SiteReport report = this.surveyEngine.analyze(gridFilled(10, 10, 64, BiomeId.of("minecraft:plains")), boundsForGrid(10, 10));

        assertTrue(engine.score(report).isEmpty());
    }

    @Test
    void clampBounds_extremeWeightsRemainBounded() {
        ScoreEngine engine = new ScoreEngine(() -> Map.of(
                DEFENSE, new ConfiguredTraitScorer(new TraitScorerConfig(
                        DEFENSE,
                        5.0f,
                        Map.of(),
                        Set.of(),
                        Set.of(),
                        Map.of(),
                        Map.of(),
                        0.0f,
                        0.0f
                ))
        ));
        SiteReport report = this.surveyEngine.analyze(gridFilled(10, 10, 64, BiomeId.of("minecraft:plains")), boundsForGrid(10, 10));

        assertEquals(1.0f, engine.score(report).get(DEFENSE), 0.0001f);
    }

    @Test
    void negativeWeights_reduceScores() {
        ScoreEngine engine = new ScoreEngine(() -> Map.of(
                FARMING, new ConfiguredTraitScorer(new TraitScorerConfig(
                        FARMING,
                        0.5f,
                        Map.of(ResourceTag.LUMBER, -0.4f),
                        Set.of(),
                        Set.of(),
                        Map.of(),
                        Map.of(),
                        0.0f,
                        0.0f
                ))
        ));

        Map<TraitId, Float> scores = engine.score(this.surveyEngine.analyze(gridFilled(50, 50, 68, BiomeId.of("minecraft:dark_forest")), boundsForGrid(50, 50)));
        assertEquals(0.14f, scores.get(FARMING), 0.0001f);
    }

    @Test
    void missingTraits_areExcludedFromOutput() {
        ScoreEngine engine = new ScoreEngine(() -> Map.of(
                LUMBER, new ConfiguredTraitScorer(new TraitScorerConfig(
                        LUMBER,
                        0.0f,
                        Map.of(ResourceTag.LUMBER, 1.0f),
                        Set.of(),
                        Set.of(),
                        Map.of(),
                        Map.of(),
                        0.0f,
                        0.0f
                ))
        ));

        Map<TraitId, Float> scores = engine.score(this.surveyEngine.analyze(gridFilled(50, 50, 68, BiomeId.of("minecraft:dark_forest")), boundsForGrid(50, 50)));
        assertEquals(1, scores.size());
        assertTrue(scores.containsKey(LUMBER));
        assertFalse(scores.containsKey(FARMING));
    }

    private Map<TraitId, TraitScorer> defaultScorers() {
        Map<TraitId, TraitScorer> scorers = new LinkedHashMap<>();
        scorers.put(LUMBER, new ConfiguredTraitScorer(new TraitScorerConfig(
                LUMBER, 0.0f, Map.of(ResourceTag.LUMBER, 0.8f, ResourceTag.FRESHWATER, 0.15f), Set.of(), Set.of(), Map.of(), Map.of(), 0.0f, 0.0f
        )));
        scorers.put(FARMING, new ConfiguredTraitScorer(new TraitScorerConfig(
                FARMING, 0.0f, Map.of(ResourceTag.FRESHWATER, 0.2f, ResourceTag.LUMBER, -0.2f), Set.of(), Set.of(ResourceTag.FROZEN), Map.of(),
                Map.of(BiomeId.of("minecraft:plains"), 0.6f, BiomeId.of("minecraft:meadow"), 0.7f, BiomeId.of("minecraft:savanna"), 0.5f, BiomeId.of("minecraft:sunflower_plains"), 0.55f),
                0.0f, 0.0f
        )));
        scorers.put(FISHING, new ConfiguredTraitScorer(new TraitScorerConfig(
                FISHING, 0.0f, Map.of(ResourceTag.FRESHWATER, 0.2f, ResourceTag.COASTAL, 0.2f), Set.of(), Set.of(),
                Map.of(WaterFeatureType.RIVER, 0.3f, WaterFeatureType.OCEAN, 0.4f, WaterFeatureType.LAKE, 0.25f), Map.of(), 0.0f, 0.0f
        )));
        scorers.put(MINING, new ConfiguredTraitScorer(new TraitScorerConfig(
                MINING, 0.0f, Map.of(ResourceTag.STONE, 0.4f, ResourceTag.ORE_BEARING, 0.35f), Set.of(), Set.of(), Map.of(), Map.of(), 0.2f, 60.0f
        )));
        scorers.put(DEFENSE, new ConfiguredTraitScorer(new TraitScorerConfig(
                DEFENSE, 0.1f, Map.of(ResourceTag.STONE, 0.15f), Set.of(), Set.of(), Map.of(), Map.of(), 0.3f, 60.0f
        )));
        return scorers;
    }

    private BiomeSurveyData lookup(BiomeId biomeId) {
        return switch (biomeId.full()) {
            case "minecraft:plains" -> new BiomeSurveyData(Map.of(ResourceTag.LUMBER, 0.1f), null, Set.of());
            case "minecraft:forest" -> new BiomeSurveyData(Map.of(ResourceTag.LUMBER, 0.7f), null, Set.of());
            case "minecraft:dark_forest" -> new BiomeSurveyData(Map.of(ResourceTag.LUMBER, 0.9f), null, Set.of());
            case "minecraft:river" -> new BiomeSurveyData(Map.of(ResourceTag.FRESHWATER, 1.0f), WaterFeatureType.RIVER, Set.of());
            case "minecraft:ocean" -> new BiomeSurveyData(Map.of(ResourceTag.COASTAL, 1.0f), WaterFeatureType.OCEAN, Set.of());
            case "minecraft:stony_peaks" ->
                    new BiomeSurveyData(Map.of(ResourceTag.STONE, 0.8f, ResourceTag.ORE_BEARING, 0.6f), null, Set.of());
            case "minecraft:snowy_plains" -> new BiomeSurveyData(Map.of(ResourceTag.FROZEN, 0.9f), null, Set.of());
            default -> BiomeSurveyData.DEFAULT;
        };
    }

    private static TerrainGrid gridFilled(int width, int depth, int height, BiomeId biomeId) {
        return TerrainGrid.of(0, 0, 1, filledSamples(width, depth, height, biomeId));
    }

    private static TerrainSample[][] filledSamples(int width, int depth, int height, BiomeId biomeId) {
        TerrainSample[][] samples = new TerrainSample[depth][width];
        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {
                samples[z][x] = new TerrainSample(height, biomeId, 0.8f);
            }
        }
        return samples;
    }

    private static SurveyBounds boundsForGrid(int width, int depth) {
        return new SurveyBounds(
                BoundingRegion.of(new BlockPosition(0, 0, 0), new BlockPosition(width - 1, 0, depth - 1)),
                BoundingRegion.of(new BlockPosition(0, 0, 0), new BlockPosition(width - 1, 0, depth - 1)),
                0
        );
    }

}
