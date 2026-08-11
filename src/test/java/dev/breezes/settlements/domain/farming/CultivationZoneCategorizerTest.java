package dev.breezes.settlements.domain.farming;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Only {@link CultivationZoneCategorizer#counts} is exercised here: it is a pure function over an
 * already-resolved {@link CultivationCellCategory}. {@code categorizeCell} itself is not
 * unit-tested — it requires live {@code BlockState} values from the block registry, which this
 * project's tests do not bootstrap (see {@code OreRegenDataManagerTest} for the same constraint).
 */
class CultivationZoneCategorizerTest {

    @Test
    void counts_needsTill_isTrue() {
        assertTrue(CultivationZoneCategorizer.counts(CultivationCellCategory.NEEDS_TILL));
    }

    @Test
    void counts_needsPlant_isTrue() {
        assertTrue(CultivationZoneCategorizer.counts(CultivationCellCategory.NEEDS_PLANT));
    }

    @Test
    void counts_needsReplant_isTrue() {
        assertTrue(CultivationZoneCategorizer.counts(CultivationCellCategory.NEEDS_REPLANT));
    }

    @Test
    void counts_occupied_isTrue() {
        // The behavior-facing isActionable() deliberately excludes OCCUPIED (nothing to do), but
        // counts() must still credit it — an already-cultivated cell is land the zone is using,
        // not land it's missing. Losing this case would make a fully-planted zone read as invalid.
        assertTrue(CultivationZoneCategorizer.counts(CultivationCellCategory.OCCUPIED));
    }

    @Test
    void counts_blocked_isFalse() {
        assertFalse(CultivationZoneCategorizer.counts(CultivationCellCategory.BLOCKED));
    }

    @Test
    void counts_skip_isFalse() {
        assertFalse(CultivationZoneCategorizer.counts(CultivationCellCategory.SKIP));
    }

}
