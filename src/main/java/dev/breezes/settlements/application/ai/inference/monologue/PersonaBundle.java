package dev.breezes.settlements.application.ai.inference.monologue;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

/**
 * Structured persona tokens for backend-owned prompt construction.
 */
@Builder
@Getter
public final class PersonaBundle {

    /**
     * Stable deterministic name for this villager, resolved from their UUID.
     * Provides identity continuity in the prompt (the service can address the villager by name
     * and reinforce persona) without requiring persistence or in-world display.
     */
    private final String name;

    private final String professionKey;

    @Singular
    private final List<String> traits;

    private final String speechStyle;

}
