package dev.breezes.settlements.application.ai.inference.monologue;

import dev.breezes.settlements.domain.ai.memory.MemoryType;
import dev.breezes.settlements.domain.ai.memory.SensedSites;
import dev.breezes.settlements.domain.ai.memory.SiteCoord;
import dev.breezes.settlements.domain.time.ClockTicks;
import net.minecraft.core.GlobalPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit tests for {@link SnapshotAssembler}'s wire-vocabulary transform — no Minecraft objects.
 * <p>
 * Site types are constructed directly via {@link MemoryType#decaying} to avoid loading
 * MemoryTypeRegistry (which triggers a Minecraft-backed static initializer).
 */
class SnapshotAssemblerTest {

    private static final MemoryType<List<GlobalPos>> MELON_TYPE =
            MemoryType.decaying("ripe_melon_sites", ClockTicks.minutes(40), 32);
    private static final MemoryType<List<GlobalPos>> ORE_TYPE =
            MemoryType.decaying("ore_sites", ClockTicks.hours(2), 32);

    @Test
    void toWireToken_stripsSitesSuffixAndUppercases() {
        // Arrange / Act / Assert
        assertEquals("RIPE_MELON", SnapshotAssembler.toWireToken("ripe_melon_sites"));
        assertEquals("ORE", SnapshotAssembler.toWireToken("ore_sites"));
        assertEquals("CULTIVATION", SnapshotAssembler.toWireToken("cultivation_sites"));
    }

    @Test
    void toSnapshot_mapsTokensAndCoordTriples() {
        // Arrange
        SensedSites sensedSites = new SensedSites(Map.of(
                MELON_TYPE, List.of(new SiteCoord(10, 64, 20)),
                ORE_TYPE, List.of(new SiteCoord(-5, 12, 7), new SiteCoord(-6, 12, 8))));

        // Act
        Snapshot snapshot = SnapshotAssembler.toSnapshot(sensedSites);

        // Assert
        Map<String, List<int[]>> sites = snapshot.getSites();
        assertEquals(2, sites.size());

        List<int[]> melon = sites.get("RIPE_MELON");
        assertEquals(1, melon.size());
        assertArrayEquals(new int[]{10, 64, 20}, melon.get(0));

        List<int[]> ore = sites.get("ORE");
        assertEquals(2, ore.size());
        assertArrayEquals(new int[]{-5, 12, 7}, ore.get(0));
        assertArrayEquals(new int[]{-6, 12, 8}, ore.get(1));
    }

    @Test
    void toSnapshot_emptyReadYieldsEmptySites() {
        // Arrange
        SensedSites sensedSites = new SensedSites(Map.of());

        // Act
        Snapshot snapshot = SnapshotAssembler.toSnapshot(sensedSites);

        // Assert — empty map preserves the required "{\"sites\":{}}" wire guarantee
        assertTrue(snapshot.getSites().isEmpty());
    }

}
