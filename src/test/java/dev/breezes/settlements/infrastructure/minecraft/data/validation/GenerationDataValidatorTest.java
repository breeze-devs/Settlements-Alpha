package dev.breezes.settlements.infrastructure.minecraft.data.validation;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.breezes.settlements.di.DaggerTestSettlementsComponent;
import dev.breezes.settlements.di.TestSettlementsComponent;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.infrastructure.minecraft.data.building.BuildingDefinitionDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.scoring.TraitScorerDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.traits.TraitDefinitionDataManager;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerationDataValidatorTest {

    private final TestSettlementsComponent component = DaggerTestSettlementsComponent.create();
    private final TraitDefinitionDataManager traitManager = this.component.traitDefinitionDataManager();
    private final TraitScorerDataManager scorerManager = this.component.traitScorerDataManager();
    private final BuildingDefinitionDataManager buildingManager = this.component.buildingDefinitionDataManager();
    private final GenerationDataValidator validator = this.component.generationDataValidator();

    @BeforeEach
    void resetManagers() {
        this.traitManager.loadForTest(Map.of(
                resource("settlements:traits/definitions/farming"), JsonParser.parseString("""
                        {
                          "id": "settlements:settlement_traits/farming",
                          "display_info": {
                            "display_name": "Farming",
                            "description": "desc",
                            "icon_item_id": "minecraft:wheat"
                          }
                        }
                        """)
        ));
    }

    @Test
    void unknown_scorer_trait_is_excluded_from_active_registry() {
        this.scorerManager.loadForTest(Map.of(
                resource("settlements:traits/scoring/farming"), scorerJson("settlements:settlement_traits/farming"),
                resource("settlements:traits/scoring/unknown"), scorerJson("settlements:settlement_traits/unknown")
        ));
        this.buildingManager.loadForTest(Map.of());

        this.validator.validateAndApply(this.traitManager, this.scorerManager, this.buildingManager);

        assertEquals(2, this.scorerManager.rawScorers().size());
        assertEquals(1, this.scorerManager.allScorers().size());
        assertTrue(this.scorerManager.allScorers().containsKey(TraitId.of("settlements:settlement_traits/farming")));
    }

    @Test
    void building_with_unknown_trait_is_excluded_from_active_registry() {
        this.scorerManager.loadForTest(Map.of());
        this.buildingManager.loadForTest(Map.of(
                resource("settlements:buildings/definitions/valid"), JsonParser.parseString("""
                        {
                          "id": "settlements:building_definitions/valid_building",
                          "placement_priority": 1,
                          "zone_tier_min": 0,
                          "zone_tier_max": 1,
                          "requires_road_frontage": false,
                          "requires_resources": [],
                          "forbidden_resources": [],
                          "trait_affinities": {
                            "settlements:settlement_traits/farming": 1.0
                          },
                          "minimum_rank": "FLAVOR",
                          "footprint_width": 2,
                          "footprint_depth": 2,
                          "npc_profession": null,
                          "npc_count": 0
                        }
                        """),
                resource("settlements:buildings/definitions/invalid"), JsonParser.parseString("""
                        {
                          "id": "settlements:building_definitions/invalid_building",
                          "placement_priority": 1,
                          "zone_tier_min": 0,
                          "zone_tier_max": 1,
                          "requires_road_frontage": false,
                          "requires_resources": [],
                          "forbidden_resources": [],
                          "trait_affinities": {
                            "settlements:settlement_traits/unknown": 1.0
                          },
                          "minimum_rank": "FLAVOR",
                          "footprint_width": 2,
                          "footprint_depth": 2,
                          "npc_profession": null,
                          "npc_count": 0
                        }
                        """)
        ));

        this.validator.validateAndApply(this.traitManager, this.scorerManager, this.buildingManager);

        assertEquals(2, this.buildingManager.rawDefinitions().size());
        assertEquals(1, this.buildingManager.allBuildings().size());
        assertTrue(this.buildingManager.byId("settlements:building_definitions/valid_building").isPresent());
        assertTrue(this.buildingManager.byId("settlements:building_definitions/invalid_building").isEmpty());
    }

    private static JsonElement scorerJson(String traitId) {
        return JsonParser.parseString("""
                {
                  "trait": "%s",
                  "base_score": 0.0,
                  "resource_tag_weights": {},
                  "required_tags": [],
                  "veto_tags": [],
                  "water_feature_weights": {},
                  "biome_weights": {},
                  "elevation_delta_weight": 0.0,
                  "elevation_delta_normalization": 0.0
                }
                """.formatted(traitId));
    }

    private static ResourceLocation resource(String value) {
        return ResourceLocation.parse(value);
    }

}
