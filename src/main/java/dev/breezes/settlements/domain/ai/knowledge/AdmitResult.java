package dev.breezes.settlements.domain.ai.knowledge;

/**
 * Outcome of a {@link VillagerKnowledgeStore#admit(KnowledgeEntry)} call.
 * <p>
 * Carrying distinct outcomes (rather than a plain boolean) lets callers react differently
 * to corroboration vs. silent deduplication.
 */
public enum AdmitResult {

    /**
     * The entry was novel and is now stored in the knowledge base.
     */
    NEW_ENTRY,

    /**
     * The entry's origin-id was already known; an independent source confirmed the same fact so
     * the existing entry's corroboration count and weight were bumped.
     */
    CORROBORATED_EXISTING,

    /**
     * The entry's origin-id was already known AND the incoming source was identical to the stored
     * source — pure no-op, no information gained.
     */
    IGNORED_DUPLICATE,

    /**
     * The entry's gossip hop count exceeded {@link KnowledgeEntry#MAX_HOP_COUNT} and was dropped
     * before any store mutation occurred.
     */
    REJECTED_HOP_CAP

}
