package dev.breezes.settlements.application.ai.inference.persona;

import lombok.Builder;
import lombok.Getter;

/**
 * Both parents' persona context for a bred villager, letting SIS bias a child's characterSketch toward (or
 * away from) inherited traits without re-deriving them itself.
 */
@Builder
@Getter
public final class PersonaLineage {

    private final PersonaLineageSide parentA;
    private final PersonaLineageSide parentB;

}
