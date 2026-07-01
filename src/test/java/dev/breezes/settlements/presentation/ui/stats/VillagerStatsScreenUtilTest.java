package dev.breezes.settlements.presentation.ui.stats;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillagerStatsScreenUtilTest {

    @Test
    void professionKeySuffix_withNamespace() {
        assertEquals("farmer", VillagerStatsUtil.professionKeySuffix("minecraft:farmer"));
    }

    @Test
    void professionKeySuffix_withoutNamespace() {
        assertEquals("farmer", VillagerStatsUtil.professionKeySuffix("farmer"));
    }

    @Test
    void professionKeySuffix_multipleColons() {
        assertEquals("farmer", VillagerStatsUtil.professionKeySuffix("mod:namespace:farmer"));
    }

    @Test
    void professionKeySuffix_emptyAfterColon() {
        assertEquals("", VillagerStatsUtil.professionKeySuffix("minecraft:"));
    }

    @Test
    void isUnemployed_none() {
        assertTrue(VillagerStatsUtil.isUnemployed("minecraft:none"));
    }

    @Test
    void isUnemployed_nitwit() {
        assertTrue(VillagerStatsUtil.isUnemployed("minecraft:nitwit"));
    }

    @Test
    void isUnemployed_farmer() {
        assertFalse(VillagerStatsUtil.isUnemployed("minecraft:farmer"));
    }

    @Test
    void isUnemployed_bareNone() {
        assertTrue(VillagerStatsUtil.isUnemployed("none"));
    }

    @Test
    void formatProfessionName_simple() {
        assertEquals("Farmer", VillagerStatsUtil.formatProfessionName("minecraft:farmer"));
    }

    @Test
    void formatProfessionName_underscore() {
        assertEquals("Stone Mason", VillagerStatsUtil.formatProfessionName("minecraft:stone_mason"));
    }

    @Test
    void formatProfessionName_noNamespace() {
        assertEquals("Librarian", VillagerStatsUtil.formatProfessionName("librarian"));
    }

    @ParameterizedTest
    @CsvSource({
            "-100, ui.settlements.stats.reputation.hostile",
            "-50, ui.settlements.stats.reputation.hostile",
            "-30, ui.settlements.stats.reputation.unfriendly",
            "-10, ui.settlements.stats.reputation.unfriendly",
            "-9, ui.settlements.stats.reputation.neutral",
            "0, ui.settlements.stats.reputation.neutral",
            "9, ui.settlements.stats.reputation.neutral",
            "10, ui.settlements.stats.reputation.friendly",
            "49, ui.settlements.stats.reputation.friendly",
            "50, ui.settlements.stats.reputation.honored",
            "99, ui.settlements.stats.reputation.honored",
            "100, ui.settlements.stats.reputation.exalted",
            "999, ui.settlements.stats.reputation.exalted"
    })
    void getReputationTitleKey_boundaries(int reputation, String expectedKey) {
        assertEquals(expectedKey, VillagerStatsUtil.getReputationTitleKey(reputation));
    }

}
