package dev.breezes.settlements.infrastructure.minecraft.data.fishing;

import com.google.gson.JsonParser;
import dev.breezes.settlements.domain.fishing.FishCatchEntry;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FishCatchDataManagerTest {

    private final FishCatchDataManager manager = new FishCatchDataManager();

    @Test
    void valid_entries_are_loaded_and_a_catch_can_be_rolled() {
        // Arrange
        this.manager.reload(Map.of(
                resource("settlements:fishing/catches/cod"), JsonParser.parseString("""
                        { "entity": "minecraft:cod", "item": "minecraft:cod", "weight": 60 }
                        """),
                resource("settlements:fishing/catches/salmon"), JsonParser.parseString("""
                        { "entity": "minecraft:salmon", "item": "minecraft:salmon", "weight": 25 }
                        """)
        ));

        // Act
        Optional<FishCatchEntry> rolled = this.manager.rollRandomCatch();

        // Assert
        assertEquals(2, this.manager.getAllEntries().size());
        assertTrue(rolled.isPresent(), "A catch must be rollable from a non-empty table");
    }

    @Test
    void non_positive_weight_rejects_the_entry() {
        // Arrange & Act
        this.manager.reload(Map.of(
                resource("settlements:fishing/catches/bad"), JsonParser.parseString("""
                        { "entity": "minecraft:cod", "item": "minecraft:cod", "weight": 0 }
                        """)
        ));

        // Assert — a non-positive weight fails the whole entry under strict decode
        assertTrue(this.manager.getAllEntries().isEmpty(), "Entry with non-positive weight must be rejected");
        assertTrue(this.manager.rollRandomCatch().isEmpty());
    }

    private static ResourceLocation resource(String value) {
        return ResourceLocation.parse(value);
    }

}
