package dev.breezes.settlements.domain.ai.memory;

import dev.breezes.settlements.domain.time.ClockTicks;
import net.minecraft.core.GlobalPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit tests for {@link SensedSites}. Built directly from SiteCoord data — no Minecraft types.
 */
class SensedSitesTest {

    // Site types constructed directly to avoid loading MemoryTypeRegistry, which would trigger
    // MemoryModuleTypeRegistry's static initializer (a Minecraft-backed class). Typed as the shared
    // MemoryType<List<GlobalPos>> supertype, matching the generalized SensedSites key.
    private static final MemoryType<List<GlobalPos>> MELON_TYPE =
            MemoryType.decaying("ripe_melon_sites", ClockTicks.minutes(40), 32);
    private static final MemoryType<List<GlobalPos>> PUMPKIN_TYPE =
            MemoryType.decaying("ripe_pumpkin_sites", ClockTicks.minutes(40), 32);

    // -- accessor: coordsByType --

    @Test
    void coordsByType_onlyReturnsPresentTypes() {
        // Arrange
        List<SiteCoord> melonCoords = List.of(new SiteCoord(10, 64, 20), new SiteCoord(30, 64, 40));
        Map<MemoryType<List<GlobalPos>>, List<SiteCoord>> input = Map.of(MELON_TYPE, melonCoords);

        // Act
        SensedSites sensedSites = new SensedSites(input);

        // Assert
        assertEquals(1, sensedSites.coordsByType().size());
        assertEquals(melonCoords, sensedSites.coordsByType().get(MELON_TYPE));
    }

    // -- accessor: coords --

    @Test
    void coords_returnsCoordsForPresentType() {
        // Arrange
        List<SiteCoord> melonCoords = List.of(new SiteCoord(10, 64, 20));
        Map<MemoryType<List<GlobalPos>>, List<SiteCoord>> input = Map.of(MELON_TYPE, melonCoords);

        // Act
        SensedSites sensedSites = new SensedSites(input);

        // Assert
        assertEquals(melonCoords, sensedSites.coords(MELON_TYPE));
    }

    @Test
    void coords_returnsEmptyListForAbsentType() {
        // Arrange
        Map<MemoryType<List<GlobalPos>>, List<SiteCoord>> input = Map.of();

        // Act
        SensedSites sensedSites = new SensedSites(input);

        // Assert
        assertTrue(sensedSites.coords(MELON_TYPE).isEmpty());
    }

    // -- accessor: count --

    @Test
    void count_returnsSizeForPresentType() {
        // Arrange
        List<SiteCoord> melonCoords = List.of(new SiteCoord(10, 64, 20), new SiteCoord(30, 64, 40));
        Map<MemoryType<List<GlobalPos>>, List<SiteCoord>> input = Map.of(MELON_TYPE, melonCoords);

        // Act
        SensedSites sensedSites = new SensedSites(input);

        // Assert
        assertEquals(2, sensedSites.count(MELON_TYPE));
    }

    @Test
    void count_returnsZeroForAbsentType() {
        // Arrange
        Map<MemoryType<List<GlobalPos>>, List<SiteCoord>> input = Map.of();

        // Act
        SensedSites sensedSites = new SensedSites(input);

        // Assert
        assertEquals(0, sensedSites.count(MELON_TYPE));
    }

    // -- accessor: isPresent --

    @Test
    void isPresent_returnsTrueWhenTypeHasSites() {
        // Arrange
        List<SiteCoord> melonCoords = List.of(new SiteCoord(10, 64, 20));
        Map<MemoryType<List<GlobalPos>>, List<SiteCoord>> input = Map.of(MELON_TYPE, melonCoords);

        // Act
        SensedSites sensedSites = new SensedSites(input);

        // Assert
        assertTrue(sensedSites.isPresent(MELON_TYPE));
    }

    @Test
    void isPresent_returnsFalseForAbsentType() {
        // Arrange
        Map<MemoryType<List<GlobalPos>>, List<SiteCoord>> input = Map.of();

        // Act
        SensedSites sensedSites = new SensedSites(input);

        // Assert
        assertFalse(sensedSites.isPresent(MELON_TYPE));
    }

    // -- immutability --

    @Test
    void coordsByType_returnsUnmodifiableMap() {
        // Arrange
        Map<MemoryType<List<GlobalPos>>, List<SiteCoord>> input = new HashMap<>();
        input.put(MELON_TYPE, List.of(new SiteCoord(10, 64, 20)));
        SensedSites sensedSites = new SensedSites(input);

        // Act & Assert: attempting to add an entry to the returned map must throw
        assertThrows(UnsupportedOperationException.class, () ->
                sensedSites.coordsByType().put(PUMPKIN_TYPE, List.of()));
    }

    @Test
    void coords_returnsUnmodifiableList() {
        // Arrange
        Map<MemoryType<List<GlobalPos>>, List<SiteCoord>> input = new HashMap<>();
        input.put(MELON_TYPE, List.of(new SiteCoord(10, 64, 20)));
        SensedSites sensedSites = new SensedSites(input);

        // Act & Assert: attempting to add an element to the returned list must throw
        assertThrows(UnsupportedOperationException.class, () ->
                sensedSites.coords(MELON_TYPE).add(new SiteCoord(0, 0, 0)));
    }

    @Test
    void construction_defensiveCopyOfInputMap() {
        // Arrange
        Map<MemoryType<List<GlobalPos>>, List<SiteCoord>> input = new HashMap<>();
        input.put(MELON_TYPE, List.of(new SiteCoord(10, 64, 20)));
        SensedSites sensedSites = new SensedSites(input);

        // Act: add a new type to the original map after construction
        input.put(PUMPKIN_TYPE, List.of(new SiteCoord(50, 64, 60)));

        // Assert: SensedSites reflects the snapshot at construction time, not the mutation
        assertFalse(sensedSites.isPresent(PUMPKIN_TYPE));
        assertEquals(1, sensedSites.coordsByType().size());
    }

    @Test
    void construction_defensiveCopyOfInputLists() {
        // Arrange
        List<SiteCoord> melonCoords = new ArrayList<>(List.of(new SiteCoord(10, 64, 20)));
        Map<MemoryType<List<GlobalPos>>, List<SiteCoord>> input = new HashMap<>();
        input.put(MELON_TYPE, melonCoords);
        SensedSites sensedSites = new SensedSites(input);

        // Act: add to the original list after construction
        melonCoords.add(new SiteCoord(99, 99, 99));

        // Assert: SensedSites count reflects the snapshot — only the original 1 coord
        assertEquals(1, sensedSites.count(MELON_TYPE));
    }

}
