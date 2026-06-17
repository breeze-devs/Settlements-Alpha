package dev.breezes.settlements.application.ai.dialogue;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;
import java.util.UUID;

/**
 * TODO:LLM this seems llm related
 * Context bundle for one villager during the evening PACKS sweep.
 * <p>
 * The sweep generates a set of candidate lines per villager. This record groups the
 * per-villager identity with its persona and grounding seeds so the provider can
 * batch-generate across the village in a single evening window.
 */
@Builder
@Getter
public final class VillagerDialogueContext {

    private final UUID villagerUuid;

    /**
     * Persona card assembled from deterministic per-villager attributes (name, profession, traits).
     */
    private final String personaCard;

    /**
     * Topic seeds from this villager's knowledge store (Phase 5/6): top entries by relevance,
     * capped at ~5 to stay within the prompt token budget.
     */
    @Singular
    private final List<String> groundingSeeds;

}
