package dev.breezes.settlements.domain.ai.knowledge;

/**
 * Lifecycle state for a {@link KnowledgeEntry} that originated from hearsay and
 * therefore requires verification via the Investigate behavior
 */
public enum KnowledgeResolution {

    /**
     * The hearsay tip has not yet been acted upon or verified
     * <p>
     * Planning and urgency scoring use the tip's {@link KnowledgeEntry#getWeight()} as-is.
     */
    UNRESOLVED,

    /**
     * Investigate found evidence that confirms the claim
     */
    CONFIRMED,

    /**
     * Investigate found the claim to be false or stale
     */
    REFUTED,
    ;

}
