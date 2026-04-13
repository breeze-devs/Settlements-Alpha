package dev.breezes.settlements.infrastructure.minecraft.data.traits;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.breezes.settlements.domain.generation.model.profile.TraitDefinition;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitDefinitionDataManagerTest {

    private final TraitDefinitionDataManager manager = new TraitDefinitionDataManager();

    @BeforeEach
    void setUp() throws IOException {
        this.manager.apply(loadDefaultEntries(), null, null);
    }

    @Test
    void loads_all_default_definitions() {
        assertEquals(15, this.manager.allTraitIds().size());
    }

    @Test
    void by_id_returns_expected_definition() {
        TraitDefinition definition = this.manager.byId(TraitId.of("settlements:settlement_traits/lumber")).orElseThrow();

        assertEquals("Lumber", definition.displayInfo().displayName());
    }

    @Test
    void nullable_custom_name_supported() {
        TraitDefinition definition = this.manager.byId(TraitId.of("settlements:settlement_traits/waypoint")).orElseThrow();
        assertEquals("Waypoint", definition.displayInfo().displayName());
        assertEquals(null, definition.displayInfo().customName());
    }

    @Test
    void malformed_id_skipped() {
        Map<ResourceLocation, JsonElement> entries = loadSingle("broken", """
                {
                  "id": "broken",
                  "display_info": {
                    "display_name": "Broken",
                    "description": "Invalid id",
                    "icon_item_id": "minecraft:barrier"
                  }
                }
                """);

        this.manager.apply(entries, null, null);

        assertTrue(this.manager.allTraitIds().isEmpty());
    }

    @Test
    void duplicate_id_keeps_later_entry() {
        Map<ResourceLocation, JsonElement> entries = new HashMap<>();
        entries.put(resource("settlements:traits/definitions/first"), JsonParser.parseString("""
                {
                  "id": "settlements:settlement_traits/lumber",
                  "display_info": {
                    "display_name": "First",
                    "description": "First",
                    "icon_item_id": "minecraft:stick"
                  }
                }
                """));
        entries.put(resource("settlements:traits/definitions/second"), JsonParser.parseString("""
                {
                  "id": "settlements:settlement_traits/lumber",
                  "display_info": {
                    "display_name": "Second",
                    "description": "Second",
                    "icon_item_id": "minecraft:iron_axe"
                  }
                }
                """));

        this.manager.apply(entries, null, null);

        assertEquals(1, this.manager.allTraitIds().size());
        assertEquals("Second", this.manager.byId(TraitId.of("settlements:settlement_traits/lumber")).orElseThrow().displayInfo().displayName());
    }

    @Test
    void registry_exposes_immutable_snapshot() {
        assertThrows(UnsupportedOperationException.class, () -> this.manager.allTraitIds().add(TraitId.of("settlements:settlement_traits/test")));
    }

    private static Map<ResourceLocation, JsonElement> loadDefaultEntries() throws IOException {
        String[] files = {
                "lumber", "mining", "farming", "fishing", "defense",
                "pastoral", "trade", "honey", "craft", "spiritual",
                "scholarly", "waypoint", "arcane", "ancient", "seafaring"
        };

        Map<ResourceLocation, JsonElement> entries = new HashMap<>();
        for (String file : files) {
            String resourcePath = "data/settlements/settlements/traits/definitions/" + file + ".json";
            try (InputStream stream = TraitDefinitionDataManagerTest.class.getClassLoader().getResourceAsStream(resourcePath)) {
                assertNotNull(stream, "Missing test resource: " + resourcePath);
                try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    entries.put(resource("settlements:traits/definitions/" + file), JsonParser.parseReader(reader));
                }
            }
        }
        return entries;
    }

    private static Map<ResourceLocation, JsonElement> loadSingle(String file, String json) {
        return Map.of(resource("settlements:traits/definitions/" + file), JsonParser.parseString(json));
    }

    private static ResourceLocation resource(String value) {
        return ResourceLocation.parse(value);
    }

}
