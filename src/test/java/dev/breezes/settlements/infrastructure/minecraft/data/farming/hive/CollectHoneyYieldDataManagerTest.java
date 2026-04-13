package dev.breezes.settlements.infrastructure.minecraft.data.farming.hive;

import com.google.gson.JsonParser;
import dev.breezes.settlements.domain.farming.hive.HiveHarvestBlockData;
import dev.breezes.settlements.domain.farming.hive.HiveHarvestItemEntry;
import dev.breezes.settlements.di.DaggerTestSettlementsComponent;
import dev.breezes.settlements.di.TestSettlementsComponent;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectHoneyYieldDataManagerTest {

    private final TestSettlementsComponent component = DaggerTestSettlementsComponent.create();
    private final CollectHoneyYieldDataManager manager = this.component.collectHoneyYieldDataManager();

    @BeforeEach
    void setUp() {
        // Arrange
        this.manager.loadForTest(Map.of(
                resource("settlements:settlements/farming/collect_honey/default"), JsonParser.parseString("""
                        {
                          "block": "default",
                          "expertise_pools": {
                            "novice": {
                              "rolls": 1,
                              "items": [
                                { "item": "minecraft:honey_bottle", "weight": 1.0, "min_count": 1, "max_count": 1 }
                              ]
                            }
                          }
                        }
                        """),
                resource("settlements:settlements/farming/collect_honey/minecraft_bee_hive"), JsonParser.parseString("""
                        {
                          "block": "minecraft:bee_hive",
                          "expertise_pools": {
                            "master": {
                              "rolls": 1,
                              "items": [
                                { "item": "minecraft:honey_bottle", "weight": 1.0, "min_count": 2, "max_count": 2 }
                              ]
                            }
                          }
                        }
                        """),
                resource("settlements:settlements/farming/collect_honey/invalid"), JsonParser.parseString("""
                        {
                          "block": "minecraft:bee_nest",
                          "expertise_pools": {
                            "novice": {
                              "rolls": 1,
                              "items": [
                                { "item": "minecraft:honey_bottle", "weight": 0.0, "min_count": 1, "max_count": 1 }
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
        List<HiveHarvestItemEntry> drops = this.manager.rollEntries("novice", "minecraft:bee_hive");

        // Assert
        assertEquals(1, drops.size());
        assertEquals("minecraft:honey_bottle", drops.getFirst().getItemId());
        assertEquals(1, drops.getFirst().getMinCount());
        assertEquals(1, drops.getFirst().getMaxCount());
    }

    @Test
    void loadForTest_skips_invalid_pool_without_positive_weight_items() {
        // Act
        Map<String, HiveHarvestBlockData> loaded = this.manager.allBlockData();

        // Assert
        assertTrue(loaded.containsKey("default"));
        assertTrue(loaded.containsKey("minecraft:bee_hive"));
        assertFalse(loaded.containsKey("minecraft:bee_nest"));
    }

    private static ResourceLocation resource(String value) {
        return ResourceLocation.parse(value);
    }

}
