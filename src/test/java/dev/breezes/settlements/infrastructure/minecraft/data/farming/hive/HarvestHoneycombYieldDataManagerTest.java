package dev.breezes.settlements.infrastructure.minecraft.data.farming.hive;

import com.google.gson.JsonParser;
import dev.breezes.settlements.di.DaggerTestSettlementsComponent;
import dev.breezes.settlements.di.TestSettlementsComponent;
import dev.breezes.settlements.domain.common.yields.WeightedYieldItem;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HarvestHoneycombYieldDataManagerTest {

    private final TestSettlementsComponent component = DaggerTestSettlementsComponent.create();
    private final HarvestHoneycombYieldDataManager manager = this.component.harvestHoneycombYieldDataManager();

    @BeforeEach
    void setUp() {
        // Arrange
        this.manager.reload(Map.of(
                resource("settlements:settlements/farming/harvest_honeycomb/default"), JsonParser.parseString("""
                        {
                          "block": "default",
                          "expertise_pools": {
                            "novice": {
                              "rolls": 2,
                              "items": [
                                { "item": "minecraft:honeycomb", "weight": 1.0, "min_count": 1, "max_count": 1 }
                              ]
                            }
                          }
                        }
                        """)
        ));
    }

    @Test
    void rollEntries_applies_roll_count_as_independent_draws() {
        // Act
        List<WeightedYieldItem> drops = this.manager.rollEntries("novice", "minecraft:bee_nest");

        // Assert
        assertEquals(2, drops.size());
        assertTrue(drops.stream().allMatch(entry -> entry.item().equals(ResourceLocation.parse("minecraft:honeycomb"))));
        assertTrue(drops.stream().allMatch(entry -> entry.minCount() == 1));
        assertTrue(drops.stream().allMatch(entry -> entry.maxCount() == 1));
    }

    private static ResourceLocation resource(String value) {
        return ResourceLocation.parse(value);
    }

}
