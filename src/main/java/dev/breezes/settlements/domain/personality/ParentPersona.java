package dev.breezes.settlements.domain.personality;

import java.util.List;

/**
 * One parent's persona snapshot captured at a bred child's birth:
 * the parent's adjectives and characterSketch, or empty/blank if the parent had no READY persona yet.
 */
public record ParentPersona(List<String> adjectives, String characterSketch) {

    public ParentPersona {
        adjectives = adjectives == null ? List.of() : List.copyOf(adjectives);
        if (characterSketch == null) {
            characterSketch = "";
        }
    }

    public static ParentPersona unknown() {
        return new ParentPersona(List.of(), "");
    }

    public boolean hasSignal() {
        return !adjectives.isEmpty() || !characterSketch.isBlank();
    }

}
