package dev.breezes.settlements.application.ai.inference.persona;

import dev.breezes.settlements.domain.personality.ParentPersona;
import dev.breezes.settlements.domain.personality.PersonaLineageSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure mapper tests -- no Minecraft objects, hand-constructed domain/wire DTOs only.
 */
class PersonaLineageMapperTest {

    @Test
    void toWire_emptySnapshotReturnsNull() {
        // Arrange
        PersonaLineageSnapshot snapshot = PersonaLineageSnapshot.empty();

        // Act
        PersonaLineage wire = PersonaLineageMapper.toWire(snapshot);

        // Assert
        assertNull(wire);
    }

    @Test
    void toWire_oneSidedSnapshotCarriesSignalOnPopulatedSideAndEmptyOnTheOther() {
        // Arrange
        PersonaLineageSnapshot snapshot = new PersonaLineageSnapshot(
                new ParentPersona(List.of("brave", "loyal"), "A gruff blacksmith."), ParentPersona.unknown());

        // Act
        PersonaLineage wire = PersonaLineageMapper.toWire(snapshot);

        // Assert
        assertEquals(List.of("brave", "loyal"), wire.getParentA().getAdjectives());
        assertEquals("A gruff blacksmith.", wire.getParentA().getCharacterSketchDigest());
        assertTrue(wire.getParentB().getAdjectives().isEmpty());
        assertEquals("", wire.getParentB().getCharacterSketchDigest());
    }

    @Test
    void toWire_bothSidedSnapshotDigestsBothSides() {
        // Arrange
        PersonaLineageSnapshot snapshot = new PersonaLineageSnapshot(
                new ParentPersona(List.of("brave"), "Parent A characterSketch."),
                new ParentPersona(List.of("kind"), "Parent B characterSketch."));

        // Act
        PersonaLineage wire = PersonaLineageMapper.toWire(snapshot);

        // Assert
        assertEquals("Parent A characterSketch.", wire.getParentA().getCharacterSketchDigest());
        assertEquals("Parent B characterSketch.", wire.getParentB().getCharacterSketchDigest());
    }

    @Test
    void toWire_longCharacterSketchIsTruncatedAtAWordBoundaryAndEndsWithEllipsis() {
        // Arrange -- 8-char units ("abcdefg "), 319 chars after trim, chosen so the 300-char cap
        // lands mid-word rather than conveniently on a space, exercising the back-up-to-boundary path.
        String longCharacterSketch = "abcdefg ".repeat(40).trim();
        PersonaLineageSnapshot snapshot = new PersonaLineageSnapshot(
                new ParentPersona(List.of("brave"), longCharacterSketch), ParentPersona.unknown());

        // Act
        PersonaLineage wire = PersonaLineageMapper.toWire(snapshot);
        String digest = wire.getParentA().getCharacterSketchDigest();

        // Assert
        assertTrue(digest.endsWith("…"));
        String withoutEllipsis = digest.substring(0, digest.length() - 1);
        assertTrue(longCharacterSketch.startsWith(withoutEllipsis), "digest must be a prefix of the original characterSketch, cut at a word boundary");
        assertTrue(withoutEllipsis.length() < 300, "the mid-word partial ('abcd') must have been backed out, not just space-trimmed");
        assertFalse(withoutEllipsis.endsWith("abcd"), "must not end mid-word");
    }

    @Test
    void toWire_shortCharacterSketchIsCarriedVerbatim() {
        // Arrange
        PersonaLineageSnapshot snapshot = new PersonaLineageSnapshot(
                new ParentPersona(List.of("brave"), "Short characterSketch."), ParentPersona.unknown());

        // Act
        PersonaLineage wire = PersonaLineageMapper.toWire(snapshot);

        // Assert
        assertEquals("Short characterSketch.", wire.getParentA().getCharacterSketchDigest());
    }

    @Test
    void toWire_blankCharacterSketchDigestsToEmptyString() {
        // Arrange -- adjectives alone are enough to give this side signal.
        PersonaLineageSnapshot snapshot = new PersonaLineageSnapshot(
                new ParentPersona(List.of("brave"), "   "), ParentPersona.unknown());

        // Act
        PersonaLineage wire = PersonaLineageMapper.toWire(snapshot);

        // Assert
        assertEquals("", wire.getParentA().getCharacterSketchDigest());
    }

}
