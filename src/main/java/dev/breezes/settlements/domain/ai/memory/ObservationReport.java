package dev.breezes.settlements.domain.ai.memory;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Builder;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Pure value type produced by the sensor after each scan cycle.
 * <p>
 * Carries observed presences (site → tick) and an optional confirmed-absence region.
 * Update rules:
 * <ol>
 *   <li>Upsert each presence: {@code site → max(existing, observedTick)} — never deletes.</li>
 *   <li>If a confirmed-absence region is present, delete remembered sites that fall inside it
 *       but were NOT in presences — the sensor looked authoritatively and found nothing there.</li>
 *   <li>TTL expiry is orthogonal and lazy (handled at read time in the store).</li>
 * </ol>
 * The sensor is stateless: one ObservationReport per scan cycle, nothing retained between scans.
 */
@Getter
@Builder
public final class ObservationReport {

    /**
     * Sites the sensor confirmed present this cycle: packed {@code BlockPos.asLong()} → observed game tick.
     * Built from the index hits for this resource this cycle.
     */
    private final Long2LongOpenHashMap presences;

    /**
     * The axis-aligned box that was authoritatively read this cycle (all sections were fresh).
     * Null when the scan was incomplete — missing sections mean we cannot confirm absence anywhere
     * in the box, so we never delete based on an incomplete scan.
     * <p>
     * Finer per-section absence is a future enhancement (P4); for now we derive this coarsely
     * from the {@code complete} flag on the query result: complete → whole scan box; not complete → null.
     */
    @Nullable
    private final ConfirmedAbsenceRegion confirmedAbsenceRegion;

    /**
     * Folds this report into the given store.
     * Pure logic: operates only on packed longs and SiteObservation — no Minecraft types.
     */
    public void update(@Nonnull Long2ObjectOpenHashMap<SiteObservation> store) {
        // Pass 1: upsert all presences — presence confirms a site, never removes.
        this.presences.long2LongEntrySet().forEach(entry -> {
            long packedPos = entry.getLongKey();
            long observedTick = entry.getLongValue();
            store.merge(packedPos, new SiteObservation(observedTick),
                    (existing, incoming) -> existing.keepFresher(incoming.lastSeenTick()));
        });

        // Pass 2: if the region is confirmed, purge remembered sites inside it that were NOT seen.
        // Sites outside the region are left alone — "I didn't look there" is not absence evidence.
        if (this.confirmedAbsenceRegion == null) {
            return;
        }

        store.long2ObjectEntrySet().removeIf(entry -> {
            long packedPos = entry.getLongKey();
            // Only delete if: (a) inside the confirmed box AND (b) not in the presences set.
            return this.confirmedAbsenceRegion.contains(packedPos)
                    && !this.presences.containsKey(packedPos);
        });
    }

}
