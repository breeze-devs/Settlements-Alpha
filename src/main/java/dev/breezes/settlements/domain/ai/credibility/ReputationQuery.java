package dev.breezes.settlements.domain.ai.credibility;

import java.util.UUID;

/**
 * Read-only seam for querying a villager's credibility record of another villager.
 */
public interface ReputationQuery {

    /**
     * Returns the credibility multiplier this villager assigns to tips from {@code sourceId}.
     * <p>
     * A value of 1.0 is neutral (unknown source or no data). Values above 1.0 indicate a
     * net-trustworthy source; below 1.0 indicates a net-unreliable source. The range is
     * [{@link CredibilityStore#MIN_MULTIPLIER}, {@link CredibilityStore#MAX_MULTIPLIER}].
     *
     * @param observerVillagerId UUID of the villager performing the query (owns the store)
     * @param sourceId           UUID of the villager whose credibility is being queried
     * @return credibility multiplier; 1.0 when no data is available
     */
    float getCredibilityMultiplier(UUID observerVillagerId, UUID sourceId);

}
