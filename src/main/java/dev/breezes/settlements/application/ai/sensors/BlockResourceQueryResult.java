package dev.breezes.settlements.application.ai.sensors;

import it.unimi.dsi.fastutil.longs.LongList;

import javax.annotation.Nonnull;

/**
 * Query result from a WorldResourceIndex sweep for a single resource.
 * <p>
 * Sites are carried as a fastutil {@link LongList} of packed BlockPos longs (BlockPos.asLong()) to
 * avoid boxing entirely until survivors are written to villager memory. Callers that need
 * a BlockPos should materialize with BlockPos.of(packed).
 * <p>
 * {@code complete} means every section in the query box was fresh — the scan is authoritative
 * over the whole box, so missing sites can be confirmed absent.
 * <p>
 * {@code truncated} means the number of in-box hits exceeded the resource's cap K — only the K
 * nearest were retained. A truncated result is NOT authoritative for absence beyond those K sites,
 * so confirmed-absence regions must not be emitted even when the scan was otherwise complete.
 */
public record BlockResourceQueryResult(@Nonnull LongList packedSites, boolean complete, boolean truncated) {
}
