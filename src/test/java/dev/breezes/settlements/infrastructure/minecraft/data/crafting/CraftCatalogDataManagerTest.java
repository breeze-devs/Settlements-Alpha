package dev.breezes.settlements.infrastructure.minecraft.data.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.breezes.settlements.domain.crafting.catalog.CraftRecipe;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CraftCatalogDataManagerTest {

    // One bundled craft_catalog JSON per crafting profession; filename matches the profession id.
    private static final String[] CRAFT_PROFESSION_FILES = {
            "armorer", "cartographer", "cleric", "farmer", "fletcher",
            "leatherworker", "librarian", "shepherd", "toolsmith", "weaponsmith"
    };

    private final CraftCatalogDataManager manager = new CraftCatalogDataManager();

    @Test
    void validJson_parsesItemAndTagInputs() {
        // Arrange & Act
        this.manager.reload(Map.of(
                resource("settlements:craft_catalog/shepherd"),
                JsonParser.parseString("""
                        {
                          "profession": "minecraft:shepherd",
                          "recipes": [
                            {
                              "id": "string",
                              "inputs": [ { "match": { "tag": "c:wools" }, "count": 4 } ],
                              "output": { "item": "minecraft:string", "count": 16 }
                            },
                            {
                              "id": "shears",
                              "inputs": [ { "match": { "item": "minecraft:iron_ingot" }, "count": 2 } ],
                              "output": { "item": "minecraft:shears", "count": 1 }
                            }
                          ]
                        }
                        """)
        ));

        // Assert
        List<CraftRecipe> recipes = this.manager.recipesFor(VillagerProfessionKey.SHEPHERD);
        assertEquals(2, recipes.size());

        CraftRecipe string = recipes.getFirst();
        assertEquals("string", string.id());
        assertEquals(16, string.output().count());
        assertEquals(ResourceLocation.parse("minecraft:string"), string.output().itemId());
        assertEquals(1, string.inputs().size());
        assertEquals(4, string.inputs().getFirst().count());
        assertTrue(string.inputs().getFirst().match() instanceof ItemMatch.TagRef,
                "wool input must parse as a tag match");

        CraftRecipe shears = recipes.get(1);
        assertTrue(shears.inputs().getFirst().match() instanceof ItemMatch.ItemRef itemRef
                        && itemRef.id().equals(ResourceLocation.parse("minecraft:iron_ingot")),
                "iron input must parse as an item match");
    }

    @Test
    void missingOutput_dropsWholeFile() {
        // Arrange & Act — one malformed recipe fails the whole file (per-file isolation)
        this.manager.reload(Map.of(
                resource("settlements:craft_catalog/invalid"),
                JsonParser.parseString("""
                        {
                          "profession": "minecraft:farmer",
                          "recipes": [
                            { "id": "no_output", "inputs": [ { "match": { "item": "minecraft:wheat" }, "count": 3 } ] }
                          ]
                        }
                        """)
        ));

        // Assert
        assertTrue(this.manager.recipesFor(VillagerProfessionKey.FARMER).isEmpty());
    }

    @Test
    void perFileErrorIsolation_validFileSurvivesInvalidNeighbor() {
        // Arrange
        Map<ResourceLocation, JsonElement> entries = new HashMap<>();
        entries.put(resource("settlements:craft_catalog/farmer"), JsonParser.parseString("""
                {
                  "profession": "minecraft:farmer",
                  "recipes": [
                    {
                      "id": "bread",
                      "inputs": [ { "match": { "item": "minecraft:wheat" }, "count": 3 } ],
                      "output": { "item": "minecraft:bread", "count": 1 }
                    }
                  ]
                }
                """));
        entries.put(resource("settlements:craft_catalog/broken"), JsonParser.parseString("""
                {
                  "profession": "minecraft:cleric",
                  "recipes": [
                    { "id": "bad", "inputs": [ { "match": {}, "count": 4 } ], "output": { "item": "minecraft:glass", "count": 4 } }
                  ]
                }
                """));

        // Act
        this.manager.reload(entries);

        // Assert
        assertFalse(this.manager.recipesFor(VillagerProfessionKey.FARMER).isEmpty());
        assertTrue(this.manager.recipesFor(VillagerProfessionKey.CLERIC).isEmpty());
    }

    @Test
    void defaultResourceFiles_allCraftProfessionsLoad() throws IOException {
        // Arrange & Act
        this.manager.reload(loadDefaultEntries());

        // Assert — guard every shipped craft catalog against parse regressions
        for (String profession : CRAFT_PROFESSION_FILES) {
            assertFalse(this.manager.recipesFor(VillagerProfessionKey.of(profession)).isEmpty(),
                    "Default craft catalog for profession '" + profession + "' failed to load");
        }
    }

    private static Map<ResourceLocation, JsonElement> loadDefaultEntries() throws IOException {
        Map<ResourceLocation, JsonElement> entries = new HashMap<>();
        for (String profession : CRAFT_PROFESSION_FILES) {
            String resourcePath = "data/settlements/settlements/craft_catalog/" + profession + ".json";
            try (InputStream stream = CraftCatalogDataManagerTest.class.getClassLoader().getResourceAsStream(resourcePath)) {
                assertNotNull(stream, "Missing test resource: " + resourcePath);
                try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    entries.put(resource("settlements:craft_catalog/" + profession), JsonParser.parseReader(reader));
                }
            }
        }
        return entries;
    }

    private static ResourceLocation resource(String id) {
        return ResourceLocation.parse(id);
    }

}
