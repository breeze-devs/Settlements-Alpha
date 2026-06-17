package dev.breezes.settlements.domain.ai.perception;

import dev.breezes.settlements.domain.ai.worldevent.WorldEvent;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventNamespace;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Stateless predicate that decides whether a {@link WorldEvent} from the bus
 * can be admitted as knowledge by a specific villager.
 * <p>
 * The gate implements the anti-telepathy rule: a villager only learns from
 * events it could plausibly have perceived. Two checks run in order:
 * <ol>
 *   <li><b>Namespace rejection</b> — {@link WorldEventNamespace#SYSTEM} events
 *       are infrastructure signals, never observable world facts.</li>
 *   <li><b>Manhattan chunk-distance rejection</b> — events whose chunk origin is
 *       farther than {@link #MAX_PERCEPTION_CHUNK_RADIUS} chunks away are skipped.
 *       Chunk coordinates come directly from the event envelope (no world lookup
 *       needed), so this check is O(1) and safe on the server thread.</li>
 * </ol>
 * <p>
 * Line-of-sight / falloff checks are not yet implemented; the radius alone provides
 * meaningful anti-telepathy without additional block lookups.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class PerceptionGate {

    /**
     * Maximum chunk-coordinate Manhattan distance for an event to be admissible.
     * At 16 blocks/chunk this is 4 chunks * 16 = 64 blocks radius
     */
    public static final int MAX_PERCEPTION_CHUNK_RADIUS = 4;

    /**
     * Returns {@code true} if a villager standing at the given chunk coordinates
     * should be able to perceive this event.
     *
     * @param event          the event to evaluate
     * @param villagerChunkX the villager's current chunk X
     * @param villagerChunkZ the villager's current chunk Z
     */
    public static boolean admits(WorldEvent event, int villagerChunkX, int villagerChunkZ) {
        if (event.getType().getNamespace() == WorldEventNamespace.SYSTEM) {
            return false;
        }

        int dx = Math.abs(event.getChunkX() - villagerChunkX);
        int dz = Math.abs(event.getChunkZ() - villagerChunkZ);
        return (dx + dz) <= MAX_PERCEPTION_CHUNK_RADIUS;
    }

}
