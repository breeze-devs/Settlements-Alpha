package dev.breezes.settlements.infrastructure.minecraft.data.smelting;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.breezes.settlements.domain.smelting.catalog.BlastOreRecipe;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlastOreRecipeDataManagerTest {

    private final BlastOreRecipeDataManager manager = new BlastOreRecipeDataManager();

    @Test
    void validJson_parsesRecipe_andDefaultsCountsToOne() {
        // Arrange & Act
        this.manager.reload(Map.of(
                resource("settlements:blast_recipes/raw_iron"),
                JsonParser.parseString("""
                        {
                          "input": "minecraft:raw_iron",
                          "output": "minecraft:iron_ingot"
                        }
                        """)
        ));

        // Assert
        assertEquals(1, this.manager.allRecipes().size());
        BlastOreRecipe recipe = this.manager.forInput(resource("minecraft:raw_iron")).orElseThrow();
        assertEquals(resource("minecraft:iron_ingot"), recipe.output());
        assertEquals(1, recipe.inputCount());
        assertEquals(1, recipe.outputCount());
    }

    @Test
    void explicitCounts_areRespected() {
        // Arrange & Act
        this.manager.reload(Map.of(
                resource("settlements:blast_recipes/raw_gold"),
                JsonParser.parseString("""
                        {
                          "input": "minecraft:raw_gold",
                          "output": "minecraft:gold_ingot",
                          "input_count": 2,
                          "output_count": 3
                        }
                        """)
        ));

        // Assert
        BlastOreRecipe recipe = this.manager.forInput(resource("minecraft:raw_gold")).orElseThrow();
        assertEquals(2, recipe.inputCount());
        assertEquals(3, recipe.outputCount());
    }

    @Test
    void malformedEntry_isSkipped_validNeighborSurvives() {
        // Arrange — the broken entry omits the required "output" field
        Map<ResourceLocation, JsonElement> entries = new HashMap<>();
        entries.put(resource("settlements:blast_recipes/valid"), JsonParser.parseString("""
                {
                  "input": "minecraft:raw_copper",
                  "output": "minecraft:copper_ingot"
                }
                """));
        entries.put(resource("settlements:blast_recipes/broken"), JsonParser.parseString("""
                {
                  "input": "minecraft:raw_iron"
                }
                """));

        // Act
        this.manager.reload(entries);

        // Assert — strict decode drops the malformed entry, its valid neighbor survives
        assertEquals(1, this.manager.allRecipes().size());
        assertTrue(this.manager.forInput(resource("minecraft:raw_copper")).isPresent());
        assertTrue(this.manager.forInput(resource("minecraft:raw_iron")).isEmpty());
    }

    private static ResourceLocation resource(String value) {
        return ResourceLocation.parse(value);
    }

}
