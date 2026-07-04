package dev.breezes.settlements.infrastructure.minecraft.data.animal;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.breezes.settlements.domain.animal.ButcherableAnimalEntry;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * findByEntityType is intentionally not covered here — it resolves BuiltInRegistries.ENTITY_TYPE,
 * a live Minecraft registry that cannot be mocked per project convention. These tests only exercise
 * the pure decode/reload path via getAllEntries().
 */
class ButcherableAnimalDataManagerTest {

    private final ButcherableAnimalDataManager manager = new ButcherableAnimalDataManager();

    @Test
    void validJson_parsesEntryWithSpeciesNoun() {
        // Arrange & Act
        this.manager.reload(Map.of(
                resource("settlements:animals/butcherable/cow"), JsonParser.parseString("""
                        {
                          "entity": "minecraft:cow",
                          "minimum_keep_count": 4,
                          "species_noun": "cows"
                        }
                        """)
        ));

        // Assert
        assertEquals(1, this.manager.getAllEntries().size());
        ButcherableAnimalEntry entry = findEntry("minecraft:cow").orElseThrow();
        assertEquals(4, entry.minimumKeepCount());
        assertEquals("cows", entry.speciesNoun());
    }

    @Test
    void absentSpeciesNoun_decodesWithNullField() {
        // Arrange & Act
        this.manager.reload(Map.of(
                resource("settlements:animals/butcherable/mystery_beast"), JsonParser.parseString("""
                        {
                          "entity": "minecraft:cow",
                          "minimum_keep_count": 2
                        }
                        """)
        ));

        // Assert
        ButcherableAnimalEntry entry = findEntry("minecraft:cow").orElseThrow();
        assertNull(entry.speciesNoun());
    }

    @Test
    void negativeMinimumKeepCount_rejectsWholeEntryButValidNeighborSurvives() {
        // Arrange
        Map<ResourceLocation, JsonElement> entries = new HashMap<>();
        entries.put(resource("settlements:animals/butcherable/valid"), JsonParser.parseString("""
                {
                  "entity": "minecraft:pig",
                  "minimum_keep_count": 3,
                  "species_noun": "pigs"
                }
                """));
        entries.put(resource("settlements:animals/butcherable/broken"), JsonParser.parseString("""
                {
                  "entity": "minecraft:sheep",
                  "minimum_keep_count": -1,
                  "species_noun": "sheep"
                }
                """));

        // Act
        this.manager.reload(entries);

        // Assert — malformed entry is skipped entirely, valid neighbor survives
        assertEquals(1, this.manager.getAllEntries().size());
        assertTrue(findEntry("minecraft:pig").isPresent());
        assertTrue(findEntry("minecraft:sheep").isEmpty());
    }

    @Test
    void missingRequiredField_rejectsWholeEntryButValidNeighborSurvives() {
        // Arrange
        Map<ResourceLocation, JsonElement> entries = new HashMap<>();
        entries.put(resource("settlements:animals/butcherable/valid"), JsonParser.parseString("""
                {
                  "entity": "minecraft:chicken",
                  "minimum_keep_count": 4,
                  "species_noun": "chickens"
                }
                """));
        entries.put(resource("settlements:animals/butcherable/broken"), JsonParser.parseString("""
                {
                  "entity": "minecraft:rabbit"
                }
                """));

        // Act
        this.manager.reload(entries);

        // Assert — malformed entry is skipped entirely, valid neighbor survives
        assertEquals(1, this.manager.getAllEntries().size());
        assertTrue(findEntry("minecraft:chicken").isPresent());
        assertTrue(findEntry("minecraft:rabbit").isEmpty());
    }

    private Optional<ButcherableAnimalEntry> findEntry(String entityId) {
        ResourceLocation id = ResourceLocation.parse(entityId);
        return this.manager.getAllEntries().stream()
                .filter(entry -> entry.entityId().equals(id))
                .findFirst();
    }

    private static ResourceLocation resource(String value) {
        return ResourceLocation.parse(value);
    }

}
