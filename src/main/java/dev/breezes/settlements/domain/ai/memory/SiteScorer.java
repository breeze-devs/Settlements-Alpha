package dev.breezes.settlements.domain.ai.memory;

/**
 * Pluggable ordering strategy for the live-sites view produced by the decaying observation store.
 * <p>
 * Lower score = higher priority (treated as a comparator key). The default implementation
 * scores by squared distance from the querying origin. Future implementations can blend
 * recency, profession-specific value, or LLM confidence without touching any reader.
 */
@FunctionalInterface
public interface SiteScorer {

    /**
     * Computes a priority score for a remembered site relative to the querying origin.
     * Lower values rank higher (closer / more preferred).
     *
     * @param packedSitePos packed {@code BlockPos.asLong()} of the remembered site
     * @param originX       X coordinate of the querying entity
     * @param originY       Y coordinate of the querying entity
     * @param originZ       Z coordinate of the querying entity
     * @param lastSeenTick  game tick when this site was last confirmed present
     * @param nowTick       current game tick (for recency calculations)
     * @return a non-negative score; lower = higher priority
     */
    double score(long packedSitePos, int originX, int originY, int originZ,
                 long lastSeenTick, long nowTick);

    /**
     * Default scorer: Euclidean squared distance from the origin to the site.
     * Ignores recency — close and fresh are correlated under frequent re-confirmation,
     * so distance alone is sufficient ordering for heuristic planners.
     */
    SiteScorer DISTANCE = (packedSitePos, originX, originY, originZ, lastSeenTick, nowTick) -> {
        // Delegate to the shared packed-pos extractor to keep bit-layout knowledge in one place.
        double dx = PackedPos.x(packedSitePos) - originX;
        double dy = PackedPos.y(packedSitePos) - originY;
        double dz = PackedPos.z(packedSitePos) - originZ;
        return dx * dx + dy * dy + dz * dz;
    };

}
