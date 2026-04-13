package dev.breezes.settlements.infrastructure.minecraft.data.history;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.breezes.settlements.domain.generation.history.HistoryEventDefinition;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.domain.generation.model.survey.ResourceTag;
import dev.breezes.settlements.domain.generation.model.survey.WaterFeatureType;
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
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HistoryEventDataManagerTest {

    private static final TraitId LUMBER = TraitId.of("settlements:settlement_traits/lumber");
    private static final TraitId DEFENSE = TraitId.of("settlements:settlement_traits/defense");

    private final HistoryEventDataManager manager = new HistoryEventDataManager();

    @BeforeEach
    void setUp() throws IOException {
        this.manager.apply(loadDefaultEntries(), null, null);
    }

    @Test
    void loadsAllDefaultEvents() {
        assertEquals(12, this.manager.allEvents().size());
    }

    @Test
    void allEventsHaveUniqueIds() {
        Set<String> ids = new HashSet<>();
        for (HistoryEventDefinition definition : this.manager.allEvents()) {
            assertTrue(ids.add(definition.id()));
        }
    }

    @Test
    void malformedJsonSkipped() throws IOException {
        Map<ResourceLocation, JsonElement> entries = loadDefaultEntries();
        entries.put(resource("settlements:history/events/bad_range"), JsonParser.parseString("""
                {
                  "id": "settlements:settlement_events/bad_range",
                  "category": "disaster",
                  "time_horizon_min": 300,
                  "time_horizon_max": 200,
                  "exclusive_tags": [],
                  "probability_weight": 1.0,
                  "preconditions": {},
                  "trait_modifiers": {},
                  "visual_markers": [],
                  "narrative_text": "bad"
                }
                """));

        this.manager.apply(entries, null, null);

        assertEquals(12, this.manager.allEvents().size());
    }

    @Test
    void unknownTraitInModifiersIsFiltered() {
        this.manager.apply(Map.of(
                resource("settlements:history/events/unknown_trait"),
                JsonParser.parseString("""
                        {
                          "id": "settlements:settlement_events/unknown_trait",
                          "category": "disaster",
                          "time_horizon_min": 200,
                          "time_horizon_max": 200,
                          "exclusive_tags": [],
                          "probability_weight": 1.0,
                          "preconditions": {
                            "min_trait_weights": {},
                            "required_resource_tags": [],
                            "required_water_features": [],
                            "min_population": 0
                          },
                          "trait_modifiers": {
                            "NOT_A_TRAIT": 0.9,
                            "settlements:settlement_traits/lumber": 0.5
                          },
                          "visual_markers": [],
                          "narrative_text": "test"
                        }
                        """)
        ), null, null);

        HistoryEventDefinition definition = this.manager.allEvents().getFirst();
        assertEquals(Map.of(LUMBER, 0.5f), definition.traitModifiers());
    }

    @Test
    void unknownResourceTagIsFiltered() {
        this.manager.apply(Map.of(
                resource("settlements:history/events/unknown_resource"),
                JsonParser.parseString("""
                        {
                          "id": "settlements:settlement_events/unknown_resource",
                          "category": "disaster",
                          "time_horizon_min": 200,
                          "time_horizon_max": 200,
                          "exclusive_tags": [],
                          "probability_weight": 1.0,
                          "preconditions": {
                            "min_trait_weights": {
                              "settlements:settlement_traits/defense": 0.2
                            },
                            "required_resource_tags": ["NOT_A_RESOURCE", "LUMBER"],
                            "required_water_features": ["RIVER"],
                            "min_population": 0
                          },
                          "trait_modifiers": {},
                          "visual_markers": [],
                          "narrative_text": "test"
                        }
                        """)
        ), null, null);

        HistoryEventDefinition definition = this.manager.allEvents().getFirst();
        assertEquals(Set.of(ResourceTag.LUMBER), definition.preconditions().requiredResourceTags());
        assertEquals(Set.of(WaterFeatureType.RIVER), definition.preconditions().requiredWaterFeatures());
        assertEquals(Map.of(DEFENSE, 0.2f), definition.preconditions().minTraitWeights());
    }

    @Test
    void preconditionsDefaultToNone() {
        this.manager.apply(Map.of(
                resource("settlements:history/events/no_preconditions"),
                JsonParser.parseString("""
                        {
                          "id": "settlements:settlement_events/no_preconditions",
                          "category": "prosperity",
                          "time_horizon_min": 200,
                          "time_horizon_max": 200,
                          "exclusive_tags": [],
                          "probability_weight": 1.0,
                          "trait_modifiers": {},
                          "visual_markers": [],
                          "narrative_text": "test"
                        }
                        """)
        ), null, null);

        HistoryEventDefinition definition = this.manager.allEvents().getFirst();
        assertEquals(Map.of(), definition.preconditions().minTraitWeights());
        assertEquals(Set.of(), definition.preconditions().requiredResourceTags());
        assertEquals(Set.of(), definition.preconditions().requiredWaterFeatures());
        assertEquals(0, definition.preconditions().minPopulation());
    }

    private static Map<ResourceLocation, JsonElement> loadDefaultEntries() throws IOException {
        String[] files = {
                "founded_by_refugees",
                "founded_by_explorers",
                "founded_by_exiles",
                "founded_by_monks",
                "founded_by_prospectors",
                "founded_on_ancient_site",
                "founded_as_outpost",
                "founded_by_fishermen",
                "great_fire",
                "flood",
                "rich_vein_struck",
                "hero_emerged"
        };

        Map<ResourceLocation, JsonElement> entries = new HashMap<>();
        for (String file : files) {
            String resourcePath = "data/settlements/settlements/history/events/" + file + ".json";
            try (InputStream stream = HistoryEventDataManagerTest.class.getClassLoader().getResourceAsStream(resourcePath)) {
                assertNotNull(stream, "Missing test resource: " + resourcePath);
                try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    entries.put(resource("settlements:history/events/" + file), JsonParser.parseReader(reader));
                }
            }
        }
        return entries;
    }

    private static ResourceLocation resource(String value) {
        return ResourceLocation.parse(value);
    }

}
