package dev.breezes.settlements.application.ai.inference.monologue;

import dev.breezes.settlements.application.ai.dialogue.DialogueFacet;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

/**
 * Structured persona tokens for backend-owned prompt construction.
 * <p>
 * Field names here are the wire keys (plain Gson, no remapping). SIS PersonaDTO
 * uses camelCase aliases from snake_case fields, so:
 * - "profession" maps to SIS persona_dto.profession
 * - "facets" maps to SIS persona_dto.facets (list of string enum names)
 * - "anchors" maps to SIS persona_dto.anchors
 * speechStyle is optional
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

    /**
     * Wire key "profession" — matches SIS PersonaDTO.profession.
     * Value is the profession registry id (e.g. "minecraft:farmer").
     */
    private final String profession;

    @Singular
    private final List<String> traits;

    private final String speechStyle;

    /**
     * Low-cardinality facet tokens nested under the persona per the SIS contract.
     * Gson serializes each DialogueFacet by its .name() (e.g. "WAS_CURED"), which is
     * exactly the key SIS's _FACET_PHRASES map expects.
     */
    @Singular
    private final List<DialogueFacet> facets;

    /**
     * Position anchors for SIS spatial grounding. Required by the SIS contract.
     */
    private final Anchors anchors;

}
