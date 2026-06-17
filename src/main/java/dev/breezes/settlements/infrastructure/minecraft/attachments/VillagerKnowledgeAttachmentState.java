package dev.breezes.settlements.infrastructure.minecraft.attachments;

import java.util.List;

/**
 * Top-level attachment state for the per-villager knowledge store.
 * Follows the same initialized/data pattern as {@link VillagerGeneticsAttachmentState}.
 */
public record VillagerKnowledgeAttachmentState(boolean initialized, List<KnowledgeEntryState> entries) {

    public static VillagerKnowledgeAttachmentState empty() {
        return new VillagerKnowledgeAttachmentState(false, List.of());
    }

    public static VillagerKnowledgeAttachmentState of(List<KnowledgeEntryState> entries) {
        return new VillagerKnowledgeAttachmentState(true, List.copyOf(entries));
    }

}
