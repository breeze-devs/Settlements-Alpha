package dev.breezes.settlements.presentation.ui.stats;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

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
    void professionTranslationKey_withNamespace() {
        assertEquals("entity.settlements.base_villager.farmer", VillagerStatsUtil.professionTranslationKey("minecraft:farmer"));
    }

    @Test
    void professionTranslationKey_noNamespace() {
        assertEquals("entity.settlements.base_villager.librarian", VillagerStatsUtil.professionTranslationKey("librarian"));
    }

    @ParameterizedTest
    @CsvSource({
            "1, merchant.level.1",
            "2, merchant.level.2",
            "3, merchant.level.3",
            "4, merchant.level.4",
            "5, merchant.level.5"
    })
    void expertiseTranslationKey_validLevels(int level, String expectedKey) {
        assertEquals(Optional.of(expectedKey), VillagerStatsUtil.expertiseTranslationKey(level));
    }

    @ParameterizedTest
    @CsvSource({"0", "6", "-1"})
    void expertiseTranslationKey_outOfRangeIsEmpty(int level) {
        assertEquals(Optional.empty(), VillagerStatsUtil.expertiseTranslationKey(level));
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
