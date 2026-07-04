package dev.breezes.settlements.application.ai.inference.persona;

import dev.breezes.settlements.domain.personality.ParentPersona;
import dev.breezes.settlements.domain.personality.PersonaLineageSnapshot;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Mapper from the domain {@link PersonaLineageSnapshot} to the wire {@link PersonaLineage}.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class PersonaLineageMapper {

    // Parent character sketches can run long; the child's prompt ships a short digest of each, not two full sketches.
    private static final int MAX_DIGEST_LENGTH = 300;

    @Nullable
    public static PersonaLineage toWire(@Nonnull PersonaLineageSnapshot snapshot) {
        if (!snapshot.hasAnySignal()) {
            return null;
        }

        return PersonaLineage.builder()
                .parentA(toSide(snapshot.parentA()))
                .parentB(toSide(snapshot.parentB()))
                .build();
    }

    private static PersonaLineageSide toSide(ParentPersona parent) {
        return PersonaLineageSide.builder()
                .adjectives(parent.adjectives())
                .characterSketchDigest(digest(parent.characterSketch()))
                .build();
    }

    private static String digest(String characterSketch) {
        String trimmed = characterSketch.trim();
        if (trimmed.isBlank()) {
            return "";
        }
        if (trimmed.length() <= MAX_DIGEST_LENGTH) {
            return trimmed;
        }

        String cut = trimmed.substring(0, MAX_DIGEST_LENGTH);
        int lastWhitespace = -1;
        for (int i = cut.length() - 1; i >= 0; i--) {
            if (Character.isWhitespace(cut.charAt(i))) {
                lastWhitespace = i;
                break;
            }
        }
        if (lastWhitespace > 0) {
            cut = cut.substring(0, lastWhitespace);
        }
        return cut + "…";
    }

}
