package dev.breezes.settlements.infrastructure.minecraft.data.trading;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeCatalogDataManagerTest {

    // One bundled JSON per profession; filename matches the VillagerProfessionKey id.
    private static final String[] DEFAULT_PROFESSION_FILES = {
            "none", "armorer", "butcher", "cartographer", "cleric", "farmer",
            "fisherman", "fletcher", "leatherworker", "librarian", "mason",
            "nitwit", "shepherd", "toolsmith", "weaponsmith"
    };

    private final TradeCatalogDataManager manager = new TradeCatalogDataManager();

    @Test
    void validJson_loadsAndNormalizesProfessionIds() {
        this.manager.loadForTest(Map.of(
                resource("settlements:trade_catalog/farmer"),
                JsonParser.parseString("""
                        {
                          "profession": "minecraft:farmer",
                          "stock": [
                            {
                              "id": "farmer_food_bundle",
                              "match": { "tag": "c:foods" },
                              "offer": {
                                "above": 9,
                                "basePrice": 12,
                                "priceJitter": 0,
                                "bundleSize": 6
                              },
                              "dump": { "above": 64 }
                            },
                            {
                              "id": "farmer_needs_shears",
                              "match": { "item": "minecraft:shears" },
                              "restock": {
                                "below": 1,
                                "buyPricePerUnit": 17,
                                "priority": 2
                              }
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
        assertEquals(64, this.manager.supplyFor(VillagerProfessionKey.FARMER)
                .getFirst()
                .dumpAbove());
        assertEquals(2, this.manager.stockPoliciesFor(VillagerProfessionKey.FARMER).size());
    }

    @Test
    void missingRequiredField_isRejected() {
        this.manager.loadForTest(Map.of(
                resource("settlements:trade_catalog/invalid"),
                JsonParser.parseString("""
                        {
                          "profession": "minecraft:farmer",
                          "stock": [
                            {
                              "id": "missing_price",
                              "match": { "item": "minecraft:bread" },
                              "offer": {
                                "above": 0,
                                "priceJitter": 0,
                                "bundleSize": 1
                              }
                            }
                          ]
                        }
                        """)
        ));

        assertTrue(this.manager.offersFor(VillagerProfessionKey.FARMER).stream()
                .noneMatch(entry -> entry.id().equals("missing_price")));
    }

    @Test
    void invalidRungOrder_isRejected() {
        this.manager.loadForTest(Map.of(
                resource("settlements:trade_catalog/invalid_order"),
                JsonParser.parseString("""
                        {
                          "profession": "minecraft:farmer",
                          "stock": [
                            {
                              "id": "restock_above_offer",
                              "match": { "item": "minecraft:wheat" },
                              "restock": {
                                "below": 32,
                                "buyPricePerUnit": 1,
                                "priority": 1
                              },
                              "offer": {
                                "above": 16,
                                "basePrice": 8,
                                "priceJitter": 0,
                                "bundleSize": 8
                              }
                            }
                          ]
                        }
                        """)
        ));

        assertTrue(this.manager.stockPoliciesFor(VillagerProfessionKey.FARMER).isEmpty());
    }

    @Test
    void defaultResourceFiles_allProfessionsLoadWithStock() throws IOException {
        this.manager.loadForTest(loadDefaultEntries());

        for (String profession : DEFAULT_PROFESSION_FILES) {
            // A parse error in a shipped file is only logged as a warning and silently drops the
            // whole profession, so guard every bundled file against regression here.
            assertFalse(this.manager.stockPoliciesFor(VillagerProfessionKey.of(profession)).isEmpty(),
                    "Default trade catalog for profession '" + profession + "' failed to load");
        }
    }

    private static Map<ResourceLocation, JsonElement> loadDefaultEntries() throws IOException {
        Map<ResourceLocation, JsonElement> entries = new HashMap<>();
        for (String profession : DEFAULT_PROFESSION_FILES) {
            String resourcePath = "data/settlements/settlements/trade_catalog/" + profession + ".json";
            try (InputStream stream = TradeCatalogDataManagerTest.class.getClassLoader().getResourceAsStream(resourcePath)) {
                assertNotNull(stream, "Missing test resource: " + resourcePath);
                try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    entries.put(resource("settlements:trade_catalog/" + profession), JsonParser.parseReader(reader));
                }
            }
        }
        return entries;
    }

    private static ResourceLocation resource(String id) {
        return ResourceLocation.parse(id);
    }

}
