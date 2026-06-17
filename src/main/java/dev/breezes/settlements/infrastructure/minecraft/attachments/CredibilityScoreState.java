package dev.breezes.settlements.infrastructure.minecraft.attachments;

import java.util.UUID;

/**
 * A single source → score pair for the credibility attachment codec.
 */
public record CredibilityScoreState(UUID sourceId, float score) {

}
