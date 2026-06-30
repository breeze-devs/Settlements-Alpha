package dev.breezes.settlements.domain.ai.memory;

/**
 * Lightweight per-site record stored in the decaying spatial memory.
 * <p>
 * Deliberately minimal — no source/hop/weight — because this store is high-cardinality
 * (thousands of block positions). The rich episodic form lives in {@code VillagerKnowledgeStore}.
 * The lastSeenTick is the only field the fold and the TTL filter need.
 */
public record SiteEntry(long lastSeenTick) {

    /**
     * Returns a new entry updated to the more recent of the two observed ticks.
     * Used by the fold's upsert: the site was confirmed at two different times,
     * so we keep the freshest view.
     */
    public SiteEntry keepFresher(long candidateTick) {
        if (candidateTick > this.lastSeenTick) {
            return new SiteEntry(candidateTick);
        }

        return this;
    }

}
