package dev.breezes.settlements.domain.ai.knowledge;

/**
 * Outcome of a {@link VillagerKnowledgeStore#admit(KnowledgeEntry)} call.
 */
public enum AdmitResult {

    /**
     * The entry was novel and is now stored in the knowledge base.
     */
    NEW_ENTRY,

    /**
     * The entry's origin id was already known — pure no-op, no information gained.
     */
    IGNORED_DUPLICATE

}
