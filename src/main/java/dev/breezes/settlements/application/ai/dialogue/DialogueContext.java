package dev.breezes.settlements.application.ai.dialogue;

import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.Set;

/**
 * Structured context for selecting a dialogue line from the fallback ladder.
 */
@Builder
@Getter
public final class DialogueContext {

    /**
     * Stable domain profession key used by SCRIPTED selection.
     */
    @Builder.Default
    private final VillagerProfessionKey profession = VillagerProfessionKey.NONE;

    /**
     * Current situation used by SCRIPTED selection.
     */
    @Builder.Default
    private final Occasion occasion = Occasion.IDLE;

    /**
     * Low-cardinality conditions used by SCRIPTED selection.
     */
    @Singular
    private final Set<DialogueFacet> facets;

}
