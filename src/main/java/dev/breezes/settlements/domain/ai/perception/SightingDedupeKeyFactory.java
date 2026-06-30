package dev.breezes.settlements.domain.ai.perception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Derives a content-addressed deduplication key for sighting world events.
 * <p>
 * Two villagers that independently observe the same entity type in the same coarse
 * spatial cell during the same time bucket produce an identical UUID. This serves two ends:
 * each villager collapses repeat and co-witnessed sightings into a single first-hand episodic
 * fact (the knowledge store keys entries by this id), and a later <em>hearsay</em> report of the
 * same fact from a different gossip source corroborates that entry instead of duplicating it.
 * Direct co-witnesses do not corroborate one another — each simply holds its own first-hand memory.
 * <p>
 * Quantization is to 2D chunk granularity (right-shift X and Z by {@link #CHUNK_SHIFT}),
 * deliberately aligned with the chunk model the PerceptionGate/WorldEvent already use.
 * Coarser cells make co-witness convergence more robust: two villagers anywhere in the
 * same 16×16 chunk column agree on "the zombie was around here" without exact coordinates.
 * The Y axis is dropped entirely — sensed sightings of the same area must not split by
 * elevation (a witness on a hill and one in a valley below should still converge).
 * Time bucketing ({@link #TIME_BUCKET_TICKS}) ensures a sighting window of roughly
 * two real-world minutes; a new window starts fresh so a second encounter later in
 * the same day is treated as a distinct episode.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class SightingDedupeKeyFactory {

    /**
     * Block-coordinate right-shift that maps fine positions to chunk cells (16 blocks).
     * Two blocks in the same chunk column differ by at most 15 in X or Z.
     */
    public static final int CHUNK_SHIFT = 4;

    /**
     * Width of the time window in server ticks within which sightings are considered
     * the same episode. 2400 ticks ≈ 2 real-world minutes at 20 tps.
     */
    public static final long TIME_BUCKET_TICKS = 2_400L;

    /**
     * Computes a stable, content-addressed UUID from entity type, 2D chunk cell, and time bucket.
     * This is the pure (Minecraft-free) entry point; callers that have block coordinates and a
     * game tick can use it directly.
     * <p>
     * The Y axis is intentionally absent: a sighting is a coarse "around here" memory, so two
     * witnesses at different elevations within the same chunk column must still converge.
     *
     * @param entityTypeId string identifier for the entity type (e.g. "minecraft:zombie")
     * @param blockX       block-coordinate X of the subject
     * @param blockZ       block-coordinate Z of the subject
     * @param gameTick     current game tick; divided by {@link #TIME_BUCKET_TICKS} to bucket
     * @return a deterministic UUID that is identical for all witnesses of the same subject
     * in the same chunk column during the same 2-minute window
     */
    public static UUID computeDedupeKey(String entityTypeId, int blockX, int blockZ, long gameTick) {
        int chunkX = blockX >> CHUNK_SHIFT;
        int chunkZ = blockZ >> CHUNK_SHIFT;
        long timeBucket = gameTick / TIME_BUCKET_TICKS;

        String key = entityTypeId + ":" + chunkX + ":" + chunkZ + ":" + timeBucket;
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

}
