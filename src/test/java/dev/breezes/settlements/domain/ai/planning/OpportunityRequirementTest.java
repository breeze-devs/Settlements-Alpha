package dev.breezes.settlements.domain.ai.planning;

import dev.breezes.settlements.domain.ai.memory.MemoryType;
import dev.breezes.settlements.domain.time.ClockTicks;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link OpportunityRequirement} variants against a fake {@link OpportunityProbe}.
 * No Minecraft objects are constructed — only the probe interface is faked.
 * InventoryItemOpportunity and JobSiteBlockOpportunity evaluate MC Items/Blocks, so those
 * are verified in-game rather than here.
 */
class OpportunityRequirementTest {

    // Constructed directly to avoid loading MemoryTypeRegistry, whose static initializer
    // triggers MemoryModuleTypeRegistry — a Minecraft-backed class.
    private static final MemoryType.DecayingSpatialMemoryType TEST_MELON_TYPE =
            MemoryType.decaying("ripe_melon_sites", ClockTicks.minutes(40), 32);
    private static final MemoryType.DecayingSpatialMemoryType TEST_TOTEM_TYPE =
            MemoryType.decaying("test_totem_sites", ClockTicks.minutes(30), 8);

    // -- KnownSiteOpportunity: present case --

    @Test
    void knownSite_isAvailable_returnsTrueWhenNamedSiteIsPresent() {
        // Arrange
        OpportunityRequirement requirement = new OpportunityRequirement.KnownSiteOpportunity(Set.of(TEST_MELON_TYPE));
        OpportunityProbe probe = fakeSiteProbe(Set.of(TEST_MELON_TYPE));

        // Act
        boolean available = requirement.isAvailable(probe);

        // Assert
        assertTrue(available);
    }

    // -- KnownSiteOpportunity: absent case --

    @Test
    void knownSite_isAvailable_returnsFalseWhenNamedSiteIsAbsent() {
        // Arrange
        OpportunityRequirement requirement = new OpportunityRequirement.KnownSiteOpportunity(Set.of(TEST_MELON_TYPE));
        OpportunityProbe probe = fakeSiteProbe(Set.of()); // no sites present

        // Act
        boolean available = requirement.isAvailable(probe);

        // Assert
        assertFalse(available);
    }

    // -- KnownSiteOpportunity: any-of semantics --

    @Test
    void knownSite_isAvailable_returnsTrueWhenAnyOfMultipleSitesPresent() {
        // Requirement declares {melon, totem}; only totem is present — any-of → available
        // Arrange
        OpportunityRequirement requirement = new OpportunityRequirement.KnownSiteOpportunity(
                Set.of(TEST_MELON_TYPE, TEST_TOTEM_TYPE));
        OpportunityProbe probe = fakeSiteProbe(Set.of(TEST_TOTEM_TYPE));

        // Act
        boolean available = requirement.isAvailable(probe);

        // Assert
        assertTrue(available);
    }

    @Test
    void knownSite_isAvailable_returnsFalseWhenNoneOfMultipleSitesPresent() {
        // Requirement declares {melon, totem}; neither is present → unavailable
        // Arrange
        OpportunityRequirement requirement = new OpportunityRequirement.KnownSiteOpportunity(
                Set.of(TEST_MELON_TYPE, TEST_TOTEM_TYPE));
        OpportunityProbe probe = fakeSiteProbe(Set.of()); // no sites

        // Act
        boolean available = requirement.isAvailable(probe);

        // Assert
        assertFalse(available);
    }

    /**
     * Builds a fake probe that only implements hasAnySite.
     * hasAnySite returns true when at least one memory in {@code memories} is also in {@code presentSites}.
     * holdsAnyItem and hasJobSiteBlock always return false — not under test here.
     */
    private static OpportunityProbe fakeSiteProbe(Set<MemoryType<List<GlobalPos>>> presentSites) {
        return new OpportunityProbe() {

            @Override
            public boolean hasAnySite(Set<MemoryType<List<GlobalPos>>> memories) {
                for (MemoryType<List<GlobalPos>> type : memories) {
                    if (presentSites.contains(type)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean holdsAnyItem(Set<Item> items) {
                return false;
            }

            @Override
            public boolean hasJobSiteBlock(Block block) {
                return false;
            }

        };
    }

    // -- Compact constructor: empty set rejection --

    @Test
    void knownSite_constructor_rejectsEmptyMemorySet() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> new OpportunityRequirement.KnownSiteOpportunity(Set.of()));
    }

    @Test
    void inventoryItem_constructor_rejectsEmptyItemSet() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> new OpportunityRequirement.InventoryItemOpportunity(Set.<Item>of()));
    }

}
