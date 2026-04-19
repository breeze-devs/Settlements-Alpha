package dev.breezes.settlements.infrastructure.minecraft.data.trading;

import com.google.gson.JsonParser;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeCatalogDataManagerTest {

    private final TradeCatalogDataManager manager = new TradeCatalogDataManager();

    @Test
    void validJson_loadsAndNormalizesProfessionIds() {
        this.manager.loadForTest(Map.of(
                resource("settlements:trade_catalog/farmer"),
                JsonParser.parseString("""
                        {
                          "profession": "minecraft:farmer",
                          "offers": [
                            {
                              "id": "farmer_food_bundle",
                              "match": { "tag": "c:foods" },
                              "bundleSize": 6,
                              "basePrice": 12,
                              "priceJitter": 0,
                              "surplusThreshold": 9
                            }
                          ],
                          "demands": [
                            {
                              "id": "farmer_needs_shears",
                              "match": { "item": "minecraft:shears" },
                              "desiredMinCount": 1,
                              "basePricePerUnit": 17,
                              "basePriority": 2
                            }
                          ]
                        }
                        """)
        ));

        assertEquals(6, this.manager.findOffers(VillagerProfessionKey.FARMER,
                        new ItemMatch.TagRef(TagKey.create(Registries.ITEM, ResourceLocation.parse("c:foods"))))
                .getFirst()
                .bundleSize());
        assertEquals(12, this.manager.findOffers(VillagerProfessionKey.FARMER,
                        new ItemMatch.TagRef(TagKey.create(Registries.ITEM, ResourceLocation.parse("c:foods"))))
                .getFirst()
                .basePrice());
        assertEquals(17, this.manager.findDemands(VillagerProfessionKey.FARMER,
                        new ItemMatch.ItemRef(ResourceLocation.parse("minecraft:shears")))
                .getFirst()
                .basePricePerUnit());
    }

    @Test
    void missingRequiredField_isRejected() {
        this.manager.loadForTest(Map.of(
                resource("settlements:trade_catalog/invalid"),
                JsonParser.parseString("""
                        {
                          "profession": "minecraft:farmer",
                          "offers": [
                            {
                              "id": "missing_price",
                              "match": { "item": "minecraft:bread" },
                              "bundleSize": 1,
                              "priceJitter": 0,
                              "surplusThreshold": 0
                            }
                          ],
                          "demands": []
                        }
                        """)
        ));

        assertTrue(this.manager.offersFor(VillagerProfessionKey.FARMER).stream()
                .noneMatch(entry -> entry.id().equals("missing_price")));
    }

    private static ResourceLocation resource(String id) {
        return ResourceLocation.parse(id);
    }

}
