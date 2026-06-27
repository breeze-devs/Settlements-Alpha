package dev.breezes.settlements.domain.ai.memory;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link DecayingSpatialObservationStore} and the underlying fold/decay logic.
 * All tests operate on packed longs and pure Java types — zero Minecraft types are used,
 * so no game environment is required.
 * <p>
 * Packed positions are computed with the same bit layout as {@code BlockPos.asLong()}:
 * X in bits [63..38], Z in bits [37..12], Y in bits [11..0] (all sign-extended 26/26/12 bits).
 */
class DecayingSpatialObservationStoreTest {

    private static final long RETENTION_TICKS = 1000L;
    private static final int MAX_ENTRIES = 8;

    // Arbitrary block positions packed without importing BlockPos.
    // Layout: X<<38 | Z<<12 | Y (all sign-extended within their bit widths).
    private static final long POS_A = packPos(10, 64, 20);
    private static final long POS_B = packPos(30, 64, 40);
    private static final long POS_C = packPos(50, 64, 60);

    // -------------------------------------------------------------------------
    // Fold: upsert-bumps-timestamp
    // -------------------------------------------------------------------------

    @Test
    void upsert_newSite_getsInserted() {
        // Arrange
        DecayingSpatialObservationStore store = new DecayingSpatialObservationStore(RETENTION_TICKS, MAX_ENTRIES);
        long tick = 100L;

        // Act
        store.update(reportWithPresences(POS_A, tick), tick);

        // Assert
        assertTrue(store.hasLiveSites(tick), "Store should report a live site after insertion");
        assertEquals(1, store.size());
    }

    @Test
    void upsert_existingSite_bumpsToFresherTick() {
        // Arrange
        DecayingSpatialObservationStore store = new DecayingSpatialObservationStore(RETENTION_TICKS, MAX_ENTRIES);
        long firstTick = 100L;
        long laterTick = 200L;
        store.update(reportWithPresences(POS_A, firstTick), firstTick);

        // Act: re-observe the same site at a later tick.
        store.update(reportWithPresences(POS_A, laterTick), laterTick);

        // Assert: at a point past firstTick+RETENTION the site must still be live,
        // proving the timestamp was updated to laterTick rather than left at firstTick.
        long checkTick = firstTick + RETENTION_TICKS + 1;
        assertTrue(store.hasLiveSites(checkTick),
                "Site timestamp should have been bumped to laterTick; entry must survive past firstTick+TTL");
        assertEquals(1, store.size(), "Upsert must not create a duplicate entry");
    }

    @Test
    void upsert_olderObservationDoesNotOverwriteFresherTimestamp() {
        // Arrange
        DecayingSpatialObservationStore store = new DecayingSpatialObservationStore(RETENTION_TICKS, MAX_ENTRIES);
        long freshTick = 500L;
        long staleTick = 100L;
        store.update(reportWithPresences(POS_A, freshTick), freshTick);

        // Act: a stale observation arrives for the same site (could be a late gossip).
        store.update(reportWithPresences(POS_A, staleTick), freshTick); // nowTick = freshTick, observedTick = staleTick

        // Assert: site must survive past staleTick+RETENTION because the fresher timestamp wins.
        long checkTick = staleTick + RETENTION_TICKS + 1;
        assertTrue(store.hasLiveSites(checkTick),
                "Fresher timestamp must be preserved; stale observation must not overwrite it");
    }

    // -------------------------------------------------------------------------
    // Fold: absence-deletes-only-inside-region
    // -------------------------------------------------------------------------

    @Test
    void absenceRegion_deletesConfirmedAbsentSiteInsideRegion() {
        // Arrange: two sites, A inside the scan box, B outside.
        // POS_A = (10, 64, 20); POS_B = (30, 64, 40) — 20+ blocks away on X and Z.
        DecayingSpatialObservationStore store = new DecayingSpatialObservationStore(RETENTION_TICKS, MAX_ENTRIES);
        long tick = 100L;
        store.update(reportWithPresences(POS_A, tick), tick);
        store.update(reportWithPresences(POS_B, tick), tick);
        assertEquals(2, store.size());

        // Act: confirmed-absence region centered on POS_A with radius 5 — covers A, not B.
        ConfirmedAbsenceRegion regionAroundA = ConfirmedAbsenceRegion.ofScanBox(10, 64, 20, 5, 5);
        ObservationReport absenceReport = ObservationReport.builder()
                .presences(new Long2LongOpenHashMap()) // nothing seen this cycle
                .confirmedAbsenceRegion(regionAroundA)
                .build();
        store.update(absenceReport, tick + 1);

        // Assert: A was absent inside the confirmed region → deleted; B outside → untouched.
        assertEquals(1, store.size(), "POS_A inside confirmed-absence region must be deleted; POS_B must remain");
    }

    @Test
    void absenceRegion_sparesSitesThatAreAlsoInPresences() {
        // Arrange: site A inside the region.
        DecayingSpatialObservationStore store = new DecayingSpatialObservationStore(RETENTION_TICKS, MAX_ENTRIES);
        long tick = 100L;
        store.update(reportWithPresences(POS_A, tick), tick);

        // Act: region covers A, but A is also in presences (re-confirmed).
        ConfirmedAbsenceRegion regionAroundA = ConfirmedAbsenceRegion.ofScanBox(10, 64, 20, 5, 5);
        Long2LongOpenHashMap presences = new Long2LongOpenHashMap();
        presences.put(POS_A, tick + 1);

        ObservationReport report = ObservationReport.builder()
                .presences(presences)
                .confirmedAbsenceRegion(regionAroundA)
                .build();
        store.update(report, tick + 1);

        // Assert: site in presences must be kept even though it's inside the absence region.
        assertEquals(1, store.size(), "Site re-confirmed in presences must not be deleted by the absence region");
    }

    @Test
    void absenceRegion_sitesOutsideRegionAreNeverDeleted() {
        // Arrange: two sites. Region covers only A.
        DecayingSpatialObservationStore store = new DecayingSpatialObservationStore(RETENTION_TICKS, MAX_ENTRIES);
        long tick = 100L;
        store.update(reportWithPresences(POS_A, tick), tick);
        store.update(reportWithPresences(POS_C, tick), tick); // POS_C = (50, 64, 60) — far from region

        ConfirmedAbsenceRegion regionAroundA = ConfirmedAbsenceRegion.ofScanBox(10, 64, 20, 5, 5);
        ObservationReport absenceReport = ObservationReport.builder()
                .presences(new Long2LongOpenHashMap())
                .confirmedAbsenceRegion(regionAroundA)
                .build();

        // Act
        store.update(absenceReport, tick + 1);

        // Assert: POS_C (outside region) must not be touched.
        assertTrue(store.hasLiveSites(tick + 1), "POS_C outside confirmed region must survive");
        assertEquals(1, store.size()); // A deleted, C retained
    }

    // -------------------------------------------------------------------------
    // Fold: non-confirmed scan never deletes
    // -------------------------------------------------------------------------

    @Test
    void incompleteReport_doesNotDeleteAnyRememberedSites() {
        // Arrange: two sites remembered.
        DecayingSpatialObservationStore store = new DecayingSpatialObservationStore(RETENTION_TICKS, MAX_ENTRIES);
        long tick = 100L;
        store.update(reportWithPresences(POS_A, tick), tick);
        store.update(reportWithPresences(POS_C, tick), tick);

        // Act: incomplete scan with no confirmed-absence region, only sees POS_B.
        Long2LongOpenHashMap presences = new Long2LongOpenHashMap();
        presences.put(POS_B, tick + 1);
        ObservationReport partialReport = ObservationReport.builder()
                .presences(presences)
                .confirmedAbsenceRegion(null) // incomplete — not authoritative over any area
                .build();
        store.update(partialReport, tick + 1);

        // Assert: A and C must still be remembered; B was newly added.
        assertEquals(3, store.size(), "Incomplete scan must never delete previously-known sites");
    }

    // -------------------------------------------------------------------------
    // TTL: entries expire lazily on read
    // -------------------------------------------------------------------------

    @Test
    void ttlExpiry_siteIsLiveJustBeforeRetentionWindow() {
        // Arrange
        DecayingSpatialObservationStore store = new DecayingSpatialObservationStore(RETENTION_TICKS, MAX_ENTRIES);
        long observedTick = 100L;
        store.update(reportWithPresences(POS_A, observedTick), observedTick);

        // Assert: just inside the TTL window.
        long justBefore = observedTick + RETENTION_TICKS - 1;
        assertTrue(store.hasLiveSites(justBefore), "Site must still be live just before TTL expiry");
    }

    @Test
    void ttlExpiry_siteIsStaleJustAfterRetentionWindow() {
        // Arrange
        DecayingSpatialObservationStore store = new DecayingSpatialObservationStore(RETENTION_TICKS, MAX_ENTRIES);
        long observedTick = 100L;
        store.update(reportWithPresences(POS_A, observedTick), observedTick);

        // Assert: just outside the TTL window.
        long justAfter = observedTick + RETENTION_TICKS + 1;
        assertFalse(store.hasLiveSites(justAfter), "Site must be expired just past the TTL window");
    }

    // -------------------------------------------------------------------------
    // Size cap: stalest-first eviction
    // -------------------------------------------------------------------------

    @Test
    void sizeCap_evictsStalestEntryWhenCapExceeded() {
        // Arrange: cap of 2 entries.
        DecayingSpatialObservationStore store = new DecayingSpatialObservationStore(RETENTION_TICKS, 2);
        long tick = 100L;
        // POS_A observed first (oldest), POS_B slightly later, POS_C latest.
        store.update(reportWithPresences(POS_A, tick), tick);
        store.update(reportWithPresences(POS_B, tick + 10), tick + 10);

        // Act: add a third — exceeds cap.
        store.update(reportWithPresences(POS_C, tick + 20), tick + 20);

        // Assert: only 2 entries remain; POS_A (the stalest) should have been evicted.
        assertEquals(2, store.size(), "Store must evict down to maxEntries after a fold that exceeds the cap");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Packs block coordinates into a long using the same bit layout as
     * {@code BlockPos.asLong(x, y, z)}, so the domain's bit-extraction logic is
     * tested without importing any Minecraft class.
     * <p>
     * Layout (same as vanilla): X in bits [63..38] (26 bits), Z in bits [37..12] (26 bits),
     * Y in bits [11..0] (12 bits), all values sign-extended within their respective widths.
     */
    static long packPos(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38)
                | ((long) (z & 0x3FFFFFF) << 12)
                | (long) (y & 0xFFF);
    }

    private static ObservationReport reportWithPresences(long packedPos, long observedTick) {
        Long2LongOpenHashMap presences = new Long2LongOpenHashMap();
        presences.put(packedPos, observedTick);
        return ObservationReport.builder()
                .presences(presences)
                .confirmedAbsenceRegion(null)
                .build();
    }

}
