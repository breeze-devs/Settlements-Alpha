package dev.breezes.settlements.infrastructure.minecraft.data.validation;

import com.google.gson.JsonParser;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.domain.generation.trait.TraitRegistry;
import dev.breezes.settlements.infrastructure.minecraft.data.building.BuildingDefinitionDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.scoring.TraitScorerDataManager;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerationDataValidatorMockitoTest {

    private final TraitScorerDataManager scorerManager = new TraitScorerDataManager();
    private final BuildingDefinitionDataManager buildingManager = new BuildingDefinitionDataManager();
    private final GenerationDataValidator validator = new GenerationDataValidator();

    @Mock
    private TraitRegistry traitRegistry;

    @BeforeEach
    void setUp() {
        this.scorerManager.loadForTest(Map.of(
                resource("settlements:traits/scoring/farming"), JsonParser.parseString("""
                        {
                          "trait": "settlements:settlement_traits/farming",
                          "base_score": 0.0,
                          "resource_tag_weights": {},
                          "required_tags": [],
                          "veto_tags": [],
                          "water_feature_weights": {},
                          "biome_weights": {},
                          "elevation_delta_weight": 0.0,
                          "elevation_delta_normalization": 0.0
                        }
                        """),
                resource("settlements:traits/scoring/unknown"), JsonParser.parseString("""
                        {
                          "trait": "settlements:settlement_traits/unknown",
                          "base_score": 0.0,
                          "resource_tag_weights": {},
                          "required_tags": [],
                          "veto_tags": [],
                          "water_feature_weights": {},
                          "biome_weights": {},
                          "elevation_delta_weight": 0.0,
                          "elevation_delta_normalization": 0.0
                        }
                        """)
        ));
        this.buildingManager.loadForTest(Map.of());
    }

    @Test
    void excludes_unknown_scorers_using_mocked_trait_registry() {
        when(this.traitRegistry.allTraitIds()).thenReturn(Set.of(TraitId.of("settlements:settlement_traits/farming")));

        this.validator.validateAndApply(this.traitRegistry, this.scorerManager, this.buildingManager);

        assertEquals(2, this.scorerManager.rawScorers().size());
        assertEquals(1, this.scorerManager.allScorers().size());
        assertTrue(this.scorerManager.allScorers().containsKey(TraitId.of("settlements:settlement_traits/farming")));
    }

    private static ResourceLocation resource(String value) {
        return ResourceLocation.parse(value);
    }

}