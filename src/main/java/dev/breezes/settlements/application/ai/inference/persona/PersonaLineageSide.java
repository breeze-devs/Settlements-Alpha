package dev.breezes.settlements.application.ai.inference.persona;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

/**
 * One parent's contribution to a bred villager's {@link PersonaLineage}: the parent's own
 * adjectives plus a short digest of its characterSketch, rather than the full characterSketch text.
 */
@Builder
@Getter
public final class PersonaLineageSide {

    @Singular
    private final List<String> adjectives;

    private final String characterSketchDigest;

}
