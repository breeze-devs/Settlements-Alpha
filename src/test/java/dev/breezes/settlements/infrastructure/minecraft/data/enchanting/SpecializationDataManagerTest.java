package dev.breezes.settlements.infrastructure.minecraft.data.enchanting;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecializationDataManagerTest {

    private final SpecializationDataManager manager = new SpecializationDataManager();

    @Test
    void validJson_parsesProfile() {
        // Arrange & Act
        this.manager.reload(Map.of(
                resource("settlements:specializations/ancient_scholar"),
                JsonParser.parseString("""
                        {
                          "id": "settlements:specializations/ancient_scholar",
                          "display_name": "Ancient Scholar",
                          "description": "Drawn to the magic of the deep dark.",
                          "enchantment_weights": {
                            "minecraft:mending": 4.0,
                            "minecraft:unbreaking": 1.5
                          }
                        }
                        """)
        ));

        // Assert
        var profile = this.manager.getProfile("settlements:specializations/ancient_scholar").orElseThrow();
        assertEquals("Ancient Scholar", profile.displayName());
        assertEquals(4.0f, profile.getWeight("minecraft:mending"));
        assertEquals(1.0f, profile.getWeight("minecraft:unknown_enchantment"));
    }

    @Test
    void missingRequiredField_rejectsWholeEntry() {
        // Arrange
        Map<ResourceLocation, JsonElement> entries = new HashMap<>();
        entries.put(resource("settlements:specializations/valid"), JsonParser.parseString("""
                {
                  "id": "settlements:specializations/valid",
                  "display_name": "Valid"
                }
                """));
        entries.put(resource("settlements:specializations/broken"), JsonParser.parseString("""
                {
                  "display_name": "Missing Id"
                }
                """));

        // Act
        this.manager.reload(entries);

        // Assert — malformed entry (missing required "id") is skipped entirely
        assertEquals(1, this.manager.getProfile("settlements:specializations/valid").map(profile -> 1).orElse(0));
        assertTrue(this.manager.getProfile("settlements:specializations/broken").isEmpty());
    }

    private static ResourceLocation resource(String value) {
        return ResourceLocation.parse(value);
    }

}
