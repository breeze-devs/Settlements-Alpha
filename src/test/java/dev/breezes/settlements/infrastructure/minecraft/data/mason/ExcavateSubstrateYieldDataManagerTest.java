package dev.breezes.settlements.infrastructure.minecraft.data.mason;

import com.google.gson.JsonParser;
import dev.breezes.settlements.domain.common.yields.WeightedYieldItem;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExcavateSubstrateYieldDataManagerTest {

    private final ExcavateSubstrateYieldDataManager manager = new ExcavateSubstrateYieldDataManager();

    @BeforeEach
    void setUp() {
        // Arrange
        this.manager.reload(Map.of(
                resource("settlements:settlements/mason/excavate_substrate/default"), JsonParser.parseString("""
                        {
                          "block": "default",
                          "expertise_pools": {
                            "novice": {
                              "rolls": 1,
                              "items": [
                                { "item": "minecraft:sand", "weight": 1.0, "min_count": 1, "max_count": 1 }
                              ]
                            }
                          }
                        }
                        """),
                resource("settlements:settlements/mason/excavate_substrate/gravel"), JsonParser.parseString("""
                        {
                          "block": "minecraft:gravel",
                          "selection_weight": 3.0,
                          "expertise_pools": {
                            "master": {
                              "rolls": 1,
                              "items": [
                                { "item": "minecraft:gravel", "weight": 1.0, "min_count": 1, "max_count": 1 }
                              ]
                            }
                          }
                        }
                        """)
        ));
    }

    @Test
    void rollEntries_falls_back_to_global_default_when_block_specific_expertise_is_missing() {
        // Act
        List<WeightedYieldItem> drops = this.manager.rollEntries("novice", "minecraft:gravel");

        // Assert
        assertEquals(1, drops.size());
        assertEquals(ResourceLocation.parse("minecraft:sand"), drops.getFirst().item());
        assertEquals(1, drops.getFirst().minCount());
        assertEquals(1, drops.getFirst().maxCount());
    }

    @Test
    void selectionWeight_returns_block_specific_weight_when_declared() {
        // Act & Assert
        assertEquals(3.0, this.manager.selectionWeight("minecraft:gravel"));
    }

    @Test
    void selectionWeight_falls_back_to_default_weight_when_block_omits_it() {
        // Act & Assert
        assertEquals(1.0, this.manager.selectionWeight("default"));
        assertEquals(1.0, this.manager.selectionWeight("minecraft:sand"));
    }

    private static ResourceLocation resource(String value) {
        return ResourceLocation.parse(value);
    }

}
