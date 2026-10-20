package dev.breezes.settlements.infrastructure.minecraft.data.naming;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.breezes.settlements.domain.settlement.naming.SettlementNamePools;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SettlementNamePoolDataManagerTest {

    private final SettlementNamePoolDataManager manager = new SettlementNamePoolDataManager();

    @Test
    void mergesEveryFile_inFileIdOrder() {
        // Arrange — handed over out of id order, and across namespaces
        Map<ResourceLocation, JsonElement> files = new LinkedHashMap<>();
        files.put(resource("settlements:default"), json("""
                {"prefixes": ["Ash"], "suffixes": ["ford"]}
                """));
        files.put(resource("apack:extra"), json("""
                {"prefixes": ["Oak"], "suffixes": ["wick"]}
                """));

        // Act
        this.manager.reload(files);

        // Assert
        assertEquals(new SettlementNamePools(List.of("Oak", "Ash"), List.of("wick", "ford")), this.manager.pools());
    }

    @Test
    void deduplicatesWordsIgnoringCase_keepingTheFirstSpellingTrimmed() {
        // Arrange
        Map<ResourceLocation, JsonElement> files = new LinkedHashMap<>();
        files.put(resource("apack:first"), json("""
                {"prefixes": [" Ash ", "Oak", "oak"], "suffixes": ["ford"]}
                """));
        files.put(resource("apack:second"), json("""
                {"prefixes": ["ASH", "Elm"], "suffixes": ["Ford", "wick"]}
                """));

        // Act
        this.manager.reload(files);

        // Assert
        assertEquals(List.of("Ash", "Oak", "Elm"), this.manager.pools().prefixes());
        assertEquals(List.of("ford", "wick"), this.manager.pools().suffixes());
    }

    @Test
    void dropsBlankWords() {
        // Act
        this.manager.reload(Map.of(resource("apack:blanks"), json("""
                {"prefixes": ["Ash", "  "], "suffixes": ["", "ford"]}
                """)));

        // Assert
        assertEquals(new SettlementNamePools(List.of("Ash"), List.of("ford")), this.manager.pools());
    }

    @Test
    void readsAnAbsentList_asEmpty_withoutDroppingTheFile() {
        // Arrange — each file carries one list only
        Map<ResourceLocation, JsonElement> files = new LinkedHashMap<>();
        files.put(resource("apack:prefixes_only"), json("""
                {"prefixes": ["Ash"]}
                """));
        files.put(resource("apack:suffixes_only"), json("""
                {"suffixes": ["ford"]}
                """));

        // Act
        this.manager.reload(files);

        // Assert
        assertEquals(new SettlementNamePools(List.of("Ash"), List.of("ford")), this.manager.pools());
    }

    @Test
    void reload_replacesThePreviousPools() {
        // Arrange — a file a later reload no longer finds must take its words with it
        this.manager.reload(Map.of(
                resource("apack:removed"), json("""
                        {"prefixes": ["Ash"], "suffixes": ["ford"]}
                        """),
                resource("apack:kept"), json("""
                        {"prefixes": ["Oak"], "suffixes": ["wick"]}
                        """)));

        // Act
        this.manager.reload(Map.of(resource("apack:kept"), json("""
                {"prefixes": ["Oak"], "suffixes": ["wick"]}
                """)));

        // Assert
        assertEquals(new SettlementNamePools(List.of("Oak"), List.of("wick")), this.manager.pools());
    }

    @Test
    void shippedPoolFile_fillsBothPools() throws IOException {
        // Arrange — a misspelled list key in the shipped file would read as an absent, empty pool
        String path = "data/settlements/settlements/names/settlements/default.json";
        JsonElement shipped;
        try (InputStream stream = SettlementNamePoolDataManagerTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "Missing shipped resource: " + path);
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                shipped = JsonParser.parseReader(reader);
            }
        }

        // Act
        this.manager.reload(Map.of(resource("settlements:default"), shipped));

        // Assert
        assertFalse(this.manager.pools().prefixes().isEmpty());
        assertFalse(this.manager.pools().suffixes().isEmpty());
    }

    private static JsonElement json(String text) {
        return JsonParser.parseString(text);
    }

    private static ResourceLocation resource(String value) {
        return ResourceLocation.parse(value);
    }

}
