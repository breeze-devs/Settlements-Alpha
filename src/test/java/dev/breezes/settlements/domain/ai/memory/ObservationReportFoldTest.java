package dev.breezes.settlements.domain.ai.memory;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.junit.jupiter.api.Test;

import static dev.breezes.settlements.domain.ai.memory.DecayingSpatialObservationStoreTest.packPos;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the pure fold logic in {@link ObservationReport#update(Long2ObjectOpenHashMap)}.
 * <p>
 * Uses a plain {@code Long2ObjectOpenHashMap} directly — no stores, no MC types.
 * This verifies the three fold rules from the v2 spec (§6) in isolation.
 */
class ObservationReportFoldTest {

    private static final long POS_A = packPos(10, 64, 20);
    private static final long POS_B = packPos(30, 64, 40);
    private static final long POS_C = packPos(50, 64, 60);

    // -------------------------------------------------------------------------
    // Rule 1: upsert — presence confirms, max(existing, incoming) timestamp
    // -------------------------------------------------------------------------

    @Test
    void fold_insertsNewPresence_whenSiteAbsent() {
        // Arrange
        Long2ObjectOpenHashMap<SiteObservation> store = new Long2ObjectOpenHashMap<>();
        ObservationReport report = reportWithPresences(POS_A, 100L);

        // Act
        report.update(store);

        // Assert
        assertNotNull(store.get(POS_A));
        assertEquals(100L, store.get(POS_A).lastSeenTick());
    }

    @Test
    void fold_upsertsPresence_bumpsToHigherTick() {
        // Arrange: existing entry at tick 50.
        Long2ObjectOpenHashMap<SiteObservation> store = new Long2ObjectOpenHashMap<>();
        store.put(POS_A, new SiteObservation(50L));
        ObservationReport report = reportWithPresences(POS_A, 200L);

        // Act
        report.update(store);

        // Assert: tick bumped to 200.
        assertEquals(200L, store.get(POS_A).lastSeenTick());
    }

    @Test
    void fold_upsert_keepsHigherExistingTick() {
        // Arrange: existing entry fresher than incoming.
        Long2ObjectOpenHashMap<SiteObservation> store = new Long2ObjectOpenHashMap<>();
        store.put(POS_A, new SiteObservation(500L));
        ObservationReport report = reportWithPresences(POS_A, 100L); // older

        // Act
        report.update(store);

        // Assert: 500 is kept (max semantics).
        assertEquals(500L, store.get(POS_A).lastSeenTick());
    }

    // -------------------------------------------------------------------------
    // Rule 2: absence-region — deletes inside-region non-presence sites
    // -------------------------------------------------------------------------

    @Test
    void fold_withAbsenceRegion_deletesAbsentSiteInsideRegion() {
        // Arrange: POS_A inside region, POS_B outside.
        Long2ObjectOpenHashMap<SiteObservation> store = new Long2ObjectOpenHashMap<>();
        store.put(POS_A, new SiteObservation(100L));
        store.put(POS_B, new SiteObservation(100L));

        ConfirmedAbsenceRegion regionAroundA = ConfirmedAbsenceRegion.ofScanBox(10, 64, 20, 5, 5);
        ObservationReport report = ObservationReport.builder()
                .presences(new Long2LongOpenHashMap()) // nothing seen
                .confirmedAbsenceRegion(regionAroundA)
                .build();

        // Act
        report.update(store);

        // Assert: A deleted (inside region, absent), B retained (outside region).
        assertNull(store.get(POS_A), "POS_A inside confirmed-absence region must be deleted");
        assertNotNull(store.get(POS_B), "POS_B outside region must not be touched");
    }

    @Test
    void fold_withAbsenceRegion_sparesSiteAlsoInPresences() {
        // Arrange: POS_A inside region AND in presences.
        Long2ObjectOpenHashMap<SiteObservation> store = new Long2ObjectOpenHashMap<>();
        store.put(POS_A, new SiteObservation(100L));

        Long2LongOpenHashMap presences = new Long2LongOpenHashMap();
        presences.put(POS_A, 101L); // re-confirmed

        ConfirmedAbsenceRegion regionAroundA = ConfirmedAbsenceRegion.ofScanBox(10, 64, 20, 5, 5);
        ObservationReport report = ObservationReport.builder()
                .presences(presences)
                .confirmedAbsenceRegion(regionAroundA)
                .build();

        // Act
        report.update(store);

        // Assert: A is in presences → kept, not deleted; timestamp bumped.
        assertNotNull(store.get(POS_A), "Site in presences must not be deleted by absence region");
        assertEquals(101L, store.get(POS_A).lastSeenTick(), "Timestamp must be updated from presences");
    }

    // -------------------------------------------------------------------------
    // Rule 3: no absence-region → never delete
    // -------------------------------------------------------------------------

    @Test
    void fold_withoutAbsenceRegion_neverDeletesExistingSites() {
        // Arrange: POS_A and POS_C remembered. Report sees only POS_B.
        Long2ObjectOpenHashMap<SiteObservation> store = new Long2ObjectOpenHashMap<>();
        store.put(POS_A, new SiteObservation(100L));
        store.put(POS_C, new SiteObservation(100L));

        Long2LongOpenHashMap presences = new Long2LongOpenHashMap();
        presences.put(POS_B, 101L);

        ObservationReport report = ObservationReport.builder()
                .presences(presences)
                .confirmedAbsenceRegion(null)
                .build();

        // Act
        report.update(store);

        // Assert: A and C retained, B added.
        assertEquals(3, store.size(), "Without absence region, no site is ever deleted");
        assertNotNull(store.get(POS_A));
        assertNotNull(store.get(POS_B));
        assertNotNull(store.get(POS_C));
    }

    // -------------------------------------------------------------------------
    // ConfirmedAbsenceRegion membership (bit-math correctness)
    // -------------------------------------------------------------------------

    @Test
    void confirmedAbsenceRegion_containsPositionsInsideBounds() {
        // Arrange: region centered at (0, 64, 0) with radius 10.
        ConfirmedAbsenceRegion region = ConfirmedAbsenceRegion.ofScanBox(0, 64, 0, 10, 5);

        // Assert: corner positions inside bounds should be contained.
        assertTrue(region.contains(packPos(0, 64, 0)), "Anchor must be contained");
        assertTrue(region.contains(packPos(10, 64, 10)), "Max corner must be contained");
        assertTrue(region.contains(packPos(-10, 59, -10)), "Min corner must be contained");
    }

    @Test
    void confirmedAbsenceRegion_excludesPositionsOutsideBounds() {
        ConfirmedAbsenceRegion region = ConfirmedAbsenceRegion.ofScanBox(0, 64, 0, 10, 5);

        assertFalse(region.contains(packPos(11, 64, 0)), "Just outside X must not be contained");
        assertFalse(region.contains(packPos(0, 70, 0)), "Just outside Y must not be contained");
        assertFalse(region.contains(packPos(0, 64, 11)), "Just outside Z must not be contained");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static ObservationReport reportWithPresences(long packedPos, long observedTick) {
        Long2LongOpenHashMap presences = new Long2LongOpenHashMap();
        presences.put(packedPos, observedTick);
        return ObservationReport.builder()
                .presences(presences)
                .confirmedAbsenceRegion(null)
                .build();
    }

}
