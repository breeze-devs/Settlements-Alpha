package dev.breezes.settlements.infrastructure.minecraft.data.mining;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.breezes.settlements.domain.mining.OreRegenEntry;
import dev.breezes.settlements.infrastructure.minecraft.blocks.DormantOreBlock;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the pure selection logic of OreRegenDataManager — host filtering and
 * weighted choice — without touching BuiltInRegistries.
 * resolveBlockState is intentionally not tested here because it requires a
 * live Minecraft registry, which cannot be mocked per project convention.
 */
class OreRegenDataManagerTest {

    private final OreRegenDataManager manager = new OreRegenDataManager();

    // -------------------------------------------------------------------------
    // Host filter correctness
    // -------------------------------------------------------------------------

    @Test
    void stone_host_never_returns_deepslate_only_entry() {
        // Arrange
        manager.loadForTest(Map.of(
                resource("stone_iron"), entry("minecraft:iron_ore", 50.0, "stone"),
                resource("deepslate_iron"), entry("minecraft:deepslate_iron_ore", 50.0, "deepslate")
        ));

        // Act — run many rolls to ensure no deepslate entry ever surfaces for a stone host
        Set<String> rolledIds = rollRepeatedly(DormantOreBlock.Host.STONE, 200);

        // Assert
        assertFalse(rolledIds.contains("minecraft:deepslate_iron_ore"),
                "Deepslate-only entry must never appear for a stone host");
        assertTrue(rolledIds.contains("minecraft:iron_ore"),
                "Stone entry must be reachable for a stone host");
    }

    @Test
    void deepslate_host_never_returns_stone_only_entry() {
        // Arrange
        manager.loadForTest(Map.of(
                resource("stone_iron"), entry("minecraft:iron_ore", 50.0, "stone"),
                resource("deepslate_iron"), entry("minecraft:deepslate_iron_ore", 50.0, "deepslate")
        ));

        // Act
        Set<String> rolledIds = rollRepeatedly(DormantOreBlock.Host.DEEPSLATE, 200);

        // Assert
        assertFalse(rolledIds.contains("minecraft:iron_ore"),
                "Stone-only entry must never appear for a deepslate host");
        assertTrue(rolledIds.contains("minecraft:deepslate_iron_ore"),
                "Deepslate entry must be reachable for a deepslate host");
    }

    @Test
    void any_host_entry_is_eligible_for_both_stone_and_deepslate() {
        // Arrange — only an "any" entry in the table
        manager.loadForTest(Map.of(
                resource("coal_any"), entry("minecraft:coal_ore", 10.0, "any")
        ));

        // Act
        Optional<OreRegenEntry> stoneRoll = manager.rollForHost(DormantOreBlock.Host.STONE);
        Optional<OreRegenEntry> deepslateRoll = manager.rollForHost(DormantOreBlock.Host.DEEPSLATE);

        // Assert
        assertTrue(stoneRoll.isPresent(), "ANY entry must be eligible for STONE host");
        assertTrue(deepslateRoll.isPresent(), "ANY entry must be eligible for DEEPSLATE host");
        assertEquals("minecraft:coal_ore", stoneRoll.get().getBlockId());
        assertEquals("minecraft:coal_ore", deepslateRoll.get().getBlockId());
    }

    @Test
    void blank_host_in_json_defaults_to_any_and_matches_both_hosts() {
        // Arrange — omit the host field (null raw host)
        manager.loadForTest(Map.of(
                resource("iron_no_host"), entry("minecraft:iron_ore", 10.0, null)
        ));

        // Act
        Optional<OreRegenEntry> stoneRoll = manager.rollForHost(DormantOreBlock.Host.STONE);
        Optional<OreRegenEntry> deepslateRoll = manager.rollForHost(DormantOreBlock.Host.DEEPSLATE);

        // Assert
        assertTrue(stoneRoll.isPresent(), "Null/blank host must be treated as ANY for STONE");
        assertTrue(deepslateRoll.isPresent(), "Null/blank host must be treated as ANY for DEEPSLATE");
    }

    // -------------------------------------------------------------------------
    // Empty / no-match table
    // -------------------------------------------------------------------------

    @Test
    void empty_table_returns_empty_optional() {
        // Arrange
        manager.loadForTest(Map.of());

        // Act
        Optional<OreRegenEntry> result = manager.rollForHost(DormantOreBlock.Host.STONE);

        // Assert
        assertFalse(result.isPresent(), "Empty table must yield Optional.empty");
    }

    @Test
    void no_compatible_entries_for_host_returns_empty_optional() {
        // Arrange — only deepslate entries, request stone
        manager.loadForTest(Map.of(
                resource("deepslate_coal"), entry("minecraft:deepslate_coal_ore", 30.0, "deepslate")
        ));

        // Act
        Optional<OreRegenEntry> result = manager.rollForHost(DormantOreBlock.Host.STONE);

        // Assert
        assertFalse(result.isPresent(), "No compatible entries must yield Optional.empty");
    }

    // -------------------------------------------------------------------------
    // Validation / error handling
    // -------------------------------------------------------------------------

    @Test
    void invalid_entries_are_skipped_and_valid_ones_loaded() {
        // Arrange — one missing block id, one zero weight, one valid
        manager.loadForTest(Map.of(
                resource("missing_block"), JsonParser.parseString("""
                        { "weight": 10, "host": "stone" }
                        """),
                resource("zero_weight"), JsonParser.parseString("""
                        { "block": "minecraft:gold_ore", "weight": 0, "host": "stone" }
                        """),
                resource("valid_coal"), JsonParser.parseString("""
                        { "block": "minecraft:coal_ore", "weight": 20, "host": "stone" }
                        """)
        ));

        // Assert
        assertEquals(1, manager.getAllEntries().size(), "Only the valid entry should be loaded");
        assertEquals("minecraft:coal_ore", manager.getAllEntries().getFirst().getBlockId());
    }

    @Test
    void unknown_host_string_is_normalised_to_any() {
        // Arrange
        manager.loadForTest(Map.of(
                resource("weird_host"), JsonParser.parseString("""
                        { "block": "minecraft:coal_ore", "weight": 10, "host": "nether" }
                        """)
        ));

        // Act — an "any"-normalised entry is eligible for both hosts
        Optional<OreRegenEntry> stoneResult = manager.rollForHost(DormantOreBlock.Host.STONE);
        Optional<OreRegenEntry> deepslateResult = manager.rollForHost(DormantOreBlock.Host.DEEPSLATE);

        // Assert
        assertTrue(stoneResult.isPresent(), "Unknown host must fall back to ANY — eligible for STONE");
        assertTrue(deepslateResult.isPresent(), "Unknown host must fall back to ANY — eligible for DEEPSLATE");
        assertEquals(OreRegenEntry.HostFilter.ANY, stoneResult.get().getHost());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static ResourceLocation resource(String name) {
        return ResourceLocation.parse("settlements:settlements/mining/ore_weights/" + name);
    }

    private static JsonElement entry(String blockId, double weight, String host) {
        String hostPart = host != null ? ", \"host\": \"" + host + "\"" : "";
        return JsonParser.parseString(
                "{ \"block\": \"" + blockId + "\", \"weight\": " + weight + hostPart + " }"
        );
    }

    /**
     * Rolls the host N times and returns the distinct block ids observed.
     * N must be high enough that probability essentially guarantees all eligible
     * entries appear at least once for a balanced two-entry table.
     */
    private Set<String> rollRepeatedly(@SuppressWarnings("SameParameterValue") DormantOreBlock.Host host, int times) {
        return IntStream.range(0, times)
                .mapToObj(i -> manager.rollForHost(host))
                .filter(Optional::isPresent)
                .map(o -> o.get().getBlockId())
                .collect(Collectors.toSet());
    }

}
