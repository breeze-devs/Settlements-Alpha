package dev.breezes.settlements.application.economy.catalog;

import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.infrastructure.minecraft.data.trading.TradeCatalogDataManager;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradePriceResolverTest {

    // Shared catalog loaded with the shepherd shears demand (basePricePerUnit=15).
    private static final TradeCatalogDataManager CATALOG = new TradeCatalogDataManager();

    static {
        CATALOG.loadForTest(Map.of(
                ResourceLocation.parse("settlements:trade_catalog/shepherd"),
                JsonParser.parseString("""
                        {
                          "profession": "minecraft:shepherd",
                          "stock": [
                            {
                              "id": "shepherd_needs_shears",
                              "match": { "item": "minecraft:shears" },
                              "restock": {
                                "below": 1,
                                "buyPricePerUnit": 15,
                                "priority": 2
                              }
                            }
                          ]
                        }
                        """)
        ));
    }

    private final TradePriceResolver resolver = new TradePriceResolver(CATALOG);

    @Test
    void knownDemandReturnsExactBasePrice() {
        int price = this.resolver.resolveDemandPrice(
                VillagerProfessionKey.SHEPHERD,
                new ItemMatch.ItemRef(ResourceLocation.parse("minecraft:shears"))
        ).orElseThrow();

        assertEquals(15, price);
    }

    @Test
    void unknownProfessionReturnsEmpty() {
        assertTrue(this.resolver.resolveDemandPrice(
                VillagerProfessionKey.NITWIT,
                new ItemMatch.ItemRef(ResourceLocation.parse("minecraft:shears"))
        ).isEmpty());
    }

    @Test
    void demandPriceIsAlwaysDeterministic() {
        TradeCatalogDataManager manager = new TradeCatalogDataManager();
        manager.loadForTest(Map.of(
                ResourceLocation.parse("settlements:trade_catalog/custom"),
                JsonParser.parseString("""
                        {
                          "profession": "minecraft:shepherd",
                          "stock": [
                            {
                              "id": "stable_price",
                              "match": { "item": "minecraft:bread" },
                              "restock": {
                                "below": 32,
                                "buyPricePerUnit": 9,
                                "priority": 5
                              }
                            }
                          ]
                        }
                        """)
        ));
        TradePriceResolver stableResolver = new TradePriceResolver(manager);

        assertEquals(9, stableResolver.resolveDemandPrice(
                VillagerProfessionKey.SHEPHERD,
                new ItemMatch.ItemRef(ResourceLocation.parse("minecraft:bread"))
        ).orElseThrow());
    }

    @Test
    void offerBundlePriceReturnsBundleBasePriceWhenJitterIsZero() {
        TradeCatalogDataManager manager = new TradeCatalogDataManager();
        manager.loadForTest(Map.of(
                ResourceLocation.parse("settlements:trade_catalog/custom_offer"),
                JsonParser.parseString("""
                        {
                          "profession": "minecraft:shepherd",
                          "stock": [
                            {
                              "id": "bundle_offer",
                              "match": { "item": "minecraft:wool" },
                              "offer": {
                                "above": 0,
                                "basePrice": 1,
                                "priceJitter": 0,
                                "bundleSize": 4
                              }
                            }
                          ]
                        }
                        """)
        ));
        TradePriceResolver stableResolver = new TradePriceResolver(manager);

        assertEquals(1, stableResolver.resolveOfferBundlePrice(
                VillagerProfessionKey.SHEPHERD,
                new ItemMatch.ItemRef(ResourceLocation.parse("minecraft:wool"))
        ).orElseThrow());
    }

}
