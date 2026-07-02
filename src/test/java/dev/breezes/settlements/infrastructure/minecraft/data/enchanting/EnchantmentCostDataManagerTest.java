package dev.breezes.settlements.infrastructure.minecraft.data.enchanting;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.breezes.settlements.domain.entities.Expertise;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantmentCostDataManagerTest {

    private final EnchantmentCostDataManager manager = new EnchantmentCostDataManager();

    @Test
    void validJson_parsesCostData() {
        // Arrange & Act
        this.manager.reload(Map.of(
                resource("settlements:enchantments/costs/aqua_affinity"),
                JsonParser.parseString("""
                        {
                          "enchantment": "minecraft:aqua_affinity",
                          "base_cost": 25,
                          "level_multiplier": 0,
                          "max_level": 1,
                          "min_tier": "apprentice"
                        }
                        """)
        ));

        // Assert
        assertEquals(1, this.manager.getAllCosts().size());
        assertEquals(25, this.manager.getCost("minecraft:aqua_affinity").orElseThrow().costForLevel(1));
        assertEquals(Expertise.APPRENTICE.ordinal(), this.manager.getCost("minecraft:aqua_affinity").orElseThrow().minTierOrdinal());
    }

    @Test
    void unknownMinTier_rejectsWholeEntry() {
        // Arrange
        Map<ResourceLocation, JsonElement> entries = new HashMap<>();
        entries.put(resource("settlements:enchantments/costs/valid"), JsonParser.parseString("""
                {
                  "enchantment": "minecraft:mending",
                  "base_cost": 30,
                  "level_multiplier": 0,
                  "max_level": 1,
                  "min_tier": "master"
                }
                """));
        entries.put(resource("settlements:enchantments/costs/broken"), JsonParser.parseString("""
                {
                  "enchantment": "minecraft:sharpness",
                  "base_cost": 10,
                  "level_multiplier": 5,
                  "max_level": 5,
                  "min_tier": "not_a_tier"
                }
                """));

        // Act
        this.manager.reload(entries);

        // Assert — malformed entry is skipped entirely, valid neighbor survives
        assertEquals(1, this.manager.getAllCosts().size());
        assertTrue(this.manager.getCost("minecraft:mending").isPresent());
        assertTrue(this.manager.getCost("minecraft:sharpness").isEmpty());
    }

    private static ResourceLocation resource(String value) {
        return ResourceLocation.parse(value);
    }

}
