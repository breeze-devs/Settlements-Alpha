package dev.breezes.settlements.infrastructure.minecraft.data.building;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.breezes.settlements.domain.generation.model.building.BuildingDefinition;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.domain.generation.model.survey.ResourceTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildingDefinitionDataManagerTest {

    private static final TraitId LUMBER = TraitId.of("settlements:settlement_traits/lumber");

    private final BuildingDefinitionDataManager manager = new BuildingDefinitionDataManager();

    @BeforeEach
    void setUp() throws IOException {
        this.manager.apply(loadDefaultEntries(), null, null);
    }

    @Test
    void loads_all_default_definitions() {
        assertEquals(15, this.manager.allBuildings().size());
    }

    @Test
    void all_buildings_have_unique_ids() {
        Set<String> ids = new HashSet<>();
        for (BuildingDefinition definition : this.manager.allBuildings()) {
            assertTrue(ids.add(definition.id()));
        }
    }

    @Test
    void constrained_have_resources() {
        assertFalse(this.manager.constrainedBuildings().isEmpty());
        assertTrue(this.manager.constrainedBuildings().stream().allMatch(definition -> !definition.requiresResources().isEmpty()));
    }

    @Test
    void unconstrained_empty_resources() {
        assertFalse(this.manager.unconstrainedBuildings().isEmpty());
        assertTrue(this.manager.unconstrainedBuildings().stream().allMatch(definition -> definition.requiresResources().isEmpty()));
    }

    @Test
    void for_trait_sorted_by_affinity() {
        List<BuildingDefinition> lumberBuildings = this.manager.forTrait(LUMBER);

        assertEquals(List.of("settlements:sawmill", "settlements:log_storage"),
                lumberBuildings.stream().map(BuildingDefinition::id).toList());
    }

    @Test
    void for_trait_excludes_unrelated() {
        List<String> lumberBuildings = this.manager.forTrait(LUMBER).stream()
                .map(BuildingDefinition::id)
                .toList();

        assertFalse(lumberBuildings.contains("settlements:mine_entrance"));
    }

    @Test
    void universal_empty_affinities() {
        assertTrue(this.manager.byId("settlements:house").orElseThrow().traitAffinities().isEmpty());
        assertTrue(this.manager.byId("settlements:well").orElseThrow().traitAffinities().isEmpty());
        assertTrue(this.manager.byId("settlements:town_hall").orElseThrow().traitAffinities().isEmpty());
    }

    @Test
    void by_id_found_and_not_found() {
        assertTrue(this.manager.byId("settlements:tavern").isPresent());
        assertTrue(this.manager.byId("settlements:not_real").isEmpty());
    }

    @Test
    void all_footprints_valid() {
        for (BuildingDefinition definition : this.manager.allBuildings()) {
            assertTrue(definition.footprint().minWidth() <= definition.footprint().maxWidth());
            assertTrue(definition.footprint().minDepth() <= definition.footprint().maxDepth());
        }
    }

    @Test
    void all_zone_preferences_valid() {
        for (BuildingDefinition definition : this.manager.allBuildings()) {
            assertTrue(definition.zoneTierPreference().minInclusive() <= definition.zoneTierPreference().maxInclusive());
            assertTrue(definition.zoneTierPreference().maxInclusive() <= 4);
        }
    }

    @Test
    void partition_is_complete() {
        assertEquals(this.manager.allBuildings().size(),
                this.manager.constrainedBuildings().size() + this.manager.unconstrainedBuildings().size());
    }

    @Test
    void malformed_json_skipped() throws IOException {
        Map<ResourceLocation, JsonElement> entries = loadDefaultEntries();
        entries.put(resource("settlements:buildings/definitions/bad_json"), JsonParser.parseString("""
                {
                  "id": "settlements:bad_json_test",
                  "placement_priority": 1,
                  "zone_tier_min": 3,
                  "zone_tier_max": 2,
                  "requires_road_frontage": false,
                  "requires_resources": [],
                  "forbidden_resources": [],
                  "trait_affinities": {},
                  "minimum_rank": "FLAVOR",
                  "footprint_min_width": 1,
                  "footprint_max_width": 1,
                  "footprint_min_depth": 1,
                  "footprint_max_depth": 1,
                  "npc_profession": null,
                  "npc_count": 0
                }
                """));

        this.manager.apply(entries, null, null);

        assertEquals(15, this.manager.allBuildings().size());
        assertTrue(this.manager.byId("settlements:bad_json_test").isEmpty());
    }

    @Test
    void missing_id_skipped() throws IOException {
        Map<ResourceLocation, JsonElement> entries = loadDefaultEntries();
        entries.put(resource("settlements:buildings/definitions/missing_id"), JsonParser.parseString("""
                {
                  "placement_priority": 1,
                  "zone_tier_min": 0,
                  "zone_tier_max": 0,
                  "requires_road_frontage": false,
                  "requires_resources": [],
                  "forbidden_resources": [],
                  "trait_affinities": {},
                  "minimum_rank": "FLAVOR",
                  "footprint_min_width": 1,
                  "footprint_max_width": 1,
                  "footprint_min_depth": 1,
                  "footprint_max_depth": 1,
                  "npc_profession": null,
                  "npc_count": 0
                }
                """));

        this.manager.apply(entries, null, null);

        assertEquals(15, this.manager.allBuildings().size());
    }

    @Test
    void unknown_trait_in_affinities() throws IOException {
        Map<ResourceLocation, JsonElement> entries = Map.of(
                resource("settlements:buildings/definitions/unknown_trait"),
                JsonParser.parseString("""
                        {
                          "id": "settlements:unknown_trait_test",
                          "placement_priority": 1,
                          "zone_tier_min": 0,
                          "zone_tier_max": 1,
                          "requires_road_frontage": false,
                          "requires_resources": [],
                          "forbidden_resources": [],
                          "trait_affinities": {
                            "NOT_A_TRAIT": 0.9,
                            "settlements:settlement_traits/lumber": 0.5
                          },
                          "minimum_rank": "FLAVOR",
                          "footprint_min_width": 1,
                          "footprint_max_width": 2,
                          "footprint_min_depth": 1,
                          "footprint_max_depth": 2,
                          "npc_profession": null,
                          "npc_count": 0
                        }
                        """)
        );

        this.manager.apply(entries, null, null);

        BuildingDefinition definition = this.manager.byId("settlements:unknown_trait_test").orElseThrow();
        assertEquals(Map.of(LUMBER, 0.5f), definition.traitAffinities());
    }

    @Test
    void unknown_resource_in_requires() {
        this.manager.apply(Map.of(
                resource("settlements:buildings/definitions/unknown_resource"),
                JsonParser.parseString("""
                        {
                          "id": "settlements:unknown_resource_test",
                          "placement_priority": 1,
                          "zone_tier_min": 0,
                          "zone_tier_max": 1,
                          "requires_road_frontage": false,
                          "requires_resources": ["NOT_A_RESOURCE", "LUMBER"],
                          "forbidden_resources": [],
                          "trait_affinities": {},
                          "minimum_rank": "FLAVOR",
                          "footprint_min_width": 1,
                          "footprint_max_width": 2,
                          "footprint_min_depth": 1,
                          "footprint_max_depth": 2,
                          "npc_profession": null,
                          "npc_count": 0
                        }
                        """
                )), null, null);

        BuildingDefinition definition = this.manager.byId("settlements:unknown_resource_test").orElseThrow();
        assertEquals(Set.of(ResourceTag.LUMBER), definition.requiresResources());
    }

    @Test
    void preferred_tags_are_parsed_as_flat_strings() {
        this.manager.apply(Map.of(
                resource("settlements:buildings/definitions/preferred_tags"),
                JsonParser.parseString("""
                        {
                          "id": "settlements:preferred_tags_test",
                          "placement_priority": 1,
                          "zone_tier_min": 0,
                          "zone_tier_max": 1,
                          "requires_road_frontage": false,
                          "requires_resources": [],
                          "forbidden_resources": [],
                          "trait_affinities": {},
                          "minimum_rank": "FLAVOR",
                          "preferred_tags": ["taiga", "charred"],
                          "footprint_min_width": 1,
                          "footprint_max_width": 2,
                          "footprint_min_depth": 1,
                          "footprint_max_depth": 2,
                          "npc_profession": null,
                          "npc_count": 0
                        }
                        """)
        ), null, null);

        BuildingDefinition definition = this.manager.byId("settlements:preferred_tags_test").orElseThrow();
        assertEquals(Set.of("taiga", "charred"), definition.preferredTags());
    }

    private static Map<ResourceLocation, JsonElement> loadDefaultEntries() throws IOException {
        String[] files = {
                "town_hall",
                "tavern",
                "market_stall",
                "well",
                "house",
                "sawmill",
                "log_storage",
                "farmhouse",
                "barn",
                "dock",
                "fish_drying_rack",
                "mine_entrance",
                "smelter",
                "watchtower",
                "barracks"
        };

        Map<ResourceLocation, JsonElement> entries = new HashMap<>();
        for (String file : files) {
            String resourcePath = "data/settlements/settlements/buildings/definitions/" + file + ".json";
            try (InputStream stream = BuildingDefinitionDataManagerTest.class.getClassLoader().getResourceAsStream(resourcePath)) {
                assertNotNull(stream, "Missing test resource: " + resourcePath);
                try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    entries.put(resource("settlements:buildings/definitions/" + file), JsonParser.parseReader(reader));
                }
            }
        }
        return entries;
    }

    private static ResourceLocation resource(String value) {
        return ResourceLocation.parse(value);
    }

}
