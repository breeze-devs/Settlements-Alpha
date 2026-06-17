package dev.breezes.settlements.infrastructure.minecraft.attachments;

import java.util.List;

/**
 * Top-level attachment state for a villager's per-observer {@link dev.breezes.settlements.domain.ai.credibility.CredibilityStore}.
 * Follows the same initialized/data pattern as {@link VillagerGeneticsAttachmentState}.
 */
public record VillagerCredibilityAttachmentState(boolean initialized, List<CredibilityScoreState> scores) {

    public static VillagerCredibilityAttachmentState empty() {
        return new VillagerCredibilityAttachmentState(false, List.of());
    }

    public static VillagerCredibilityAttachmentState of(List<CredibilityScoreState> scores) {
        return new VillagerCredibilityAttachmentState(true, List.copyOf(scores));
    }

}
