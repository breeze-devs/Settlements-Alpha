package dev.breezes.settlements.infrastructure.generation.debug;

import dev.breezes.settlements.domain.common.BiomeId;
import dev.breezes.settlements.domain.generation.history.VisualMarkerSet;
import dev.breezes.settlements.domain.generation.history.HistoryEventResult;
import dev.breezes.settlements.domain.generation.layout.LayoutResult;
import dev.breezes.settlements.domain.generation.model.GenerationResult;
import dev.breezes.settlements.domain.generation.model.geometry.BlockPosition;
import dev.breezes.settlements.domain.generation.model.geometry.BoundingRegion;
import dev.breezes.settlements.domain.generation.model.profile.DefenseLevel;
import dev.breezes.settlements.domain.generation.model.profile.ScaleTier;
import dev.breezes.settlements.domain.generation.model.profile.SettlementProfile;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.domain.generation.model.survey.ElevationStats;
import dev.breezes.settlements.domain.generation.model.survey.SiteReport;
import dev.breezes.settlements.domain.generation.model.survey.SurveyBounds;
import dev.breezes.settlements.domain.generation.model.survey.TerrainGrid;
import dev.breezes.settlements.domain.generation.model.survey.TerrainSample;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerationResultSerializerTest {

    private static final TraitId FARMING = TraitId.of("settlements:settlement_traits/farming");

    @Test
    void toJson_includesNestedHistoryPayload() {
        SiteReport report = new SiteReport(
                SurveyBounds.fromBuildArea(BoundingRegion.of(new BlockPosition(0, 64, 0), new BlockPosition(31, 64, 31)), 0),
                TerrainGrid.of(0, 0, 1, new TerrainSample[][]{{
                        new TerrainSample(64, BiomeId.of("minecraft:plains"), 1.0f)
                }}),
                new ElevationStats(64, 70, 66, new BlockPosition(5, 70, 5)),
                Map.of(),
                Map.of(BiomeId.of("minecraft:plains"), 1.0f),
                Set.of(),
                Set.of());

        SettlementProfile profile = SettlementProfile.builder()
                .primary(FARMING)
                .secondary(List.of())
                .flavor(List.of())
                .adjustedWeights(Map.of(FARMING, 0.75f))
                .scaleTier(ScaleTier.VILLAGE)
                .estimatedPopulation(42)
                .wealthLevel(0.5f)
                .defenseLevel(DefenseLevel.LOW)
                .seed(123L)
                .historyEventIds(List.of("settlements:settlement_events/founded_by_explorers"))
                .build();

        HistoryEventResult history = new HistoryEventResult(
                List.of("settlements:settlement_events/founded_by_explorers"),
                Map.of(FARMING, 0.75f),
                new VisualMarkerSet(Set.of("explorer_camp", "flagpole")));

        LayoutResult layout = new LayoutResult(
                new BlockPosition(10, 64, 10),
                List.of(),
                List.of());

        GenerationResult result = new GenerationResult(report, profile, history, layout, 999L);

        String json = GenerationResultSerializer.toJson(result);

        assertTrue(json.contains("\"history\""));
        assertTrue(json.contains("\"eventIds\""));
        assertTrue(json.contains("settlements:settlement_events/founded_by_explorers"));
        assertTrue(json.contains("\"visualMarkers\""));
        assertTrue(json.contains("explorer_camp"));
        assertTrue(json.contains("\"modifiedWeights\""));
    }
}
