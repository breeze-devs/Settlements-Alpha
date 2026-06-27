package dev.breezes.settlements.application.ai.sensors;

import dev.breezes.settlements.domain.ai.memory.ConfirmedAbsenceRegion;
import dev.breezes.settlements.domain.ai.memory.IMemoryWrite;
import dev.breezes.settlements.domain.ai.memory.MemoryType;
import dev.breezes.settlements.domain.ai.memory.ObservationReport;
import dev.breezes.settlements.domain.ai.memory.ObservationUpdateWrite;
import dev.breezes.settlements.domain.world.blocks.BlockMatcher;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.core.BlockPos;

import javax.annotation.Nonnull;
import java.util.Optional;

public record BlockResource(@Nonnull BlockMatcher matcher,
                            @Nonnull MemoryType.DecayingSpatialMemoryType memoryType) {

    /**
     * Produces an observation-update write from an index query result.
     * <p>
     * Returns empty only when there are no presences AND no confirmed-absence region —
     * an absence-only write (no presences, non-null region) must still be emitted so that
     * remembered sites inside the region are purged after the field is fully harvested.
     * A truncated result suppresses the absence region because the scan is not authoritative
     * beyond the K nearest hits.
     */
    public Optional<IMemoryWrite> toMemoryWrite(@Nonnull BlockResourceQueryResult result,
                                                @Nonnull BlockPos anchor,
                                                long nowTick,
                                                int scanRangeHorizontal,
                                                int scanRangeVertical) {
        Long2LongOpenHashMap presences = new Long2LongOpenHashMap(result.packedSites().size());
        LongIterator it = result.packedSites().iterator();
        while (it.hasNext()) {
            presences.put(it.nextLong(), nowTick);
        }

        // Emit a confirmed-absence region only when the scan covered the full box AND was not
        // truncated. A truncated scan only knows the K nearest hits — it is not authoritative
        // about what was absent beyond those K, so claiming absence would incorrectly delete
        // valid remembered sites that simply ranked outside the cap.
        ConfirmedAbsenceRegion region = (result.complete() && !result.truncated())
                ? ConfirmedAbsenceRegion.ofScanBox(anchor.getX(), anchor.getY(), anchor.getZ(), scanRangeHorizontal, scanRangeVertical)
                : null;

        // Skip the write only when there is genuinely no information: no presences seen and no
        // confirmed-absence region to apply. An empty-presences report with a non-null region
        // is still meaningful — it deletes remembered sites that were not observed.
        if (presences.isEmpty() && region == null) {
            return Optional.empty();
        }

        ObservationReport report = ObservationReport.builder()
                .presences(presences)
                .confirmedAbsenceRegion(region)
                .build();

        return Optional.of(new ObservationUpdateWrite(this.memoryType, report, nowTick));
    }

}
