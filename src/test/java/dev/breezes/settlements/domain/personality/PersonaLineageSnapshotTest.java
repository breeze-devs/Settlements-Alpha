package dev.breezes.settlements.domain.personality;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonaLineageSnapshotTest {

    @Test
    void empty_hasNoSignal() {
        // Arrange, Act
        PersonaLineageSnapshot snapshot = PersonaLineageSnapshot.empty();

        // Assert
        assertFalse(snapshot.hasAnySignal());
    }

    @Test
    void constructor_nullSideCoercesToUnknown() {
        // Arrange, Act
        PersonaLineageSnapshot snapshot = new PersonaLineageSnapshot(null, null);

        // Assert
        assertEquals(ParentPersona.unknown(), snapshot.parentA());
        assertEquals(ParentPersona.unknown(), snapshot.parentB());
    }

    @Test
    void hasAnySignal_trueWhenOnlyParentAHasSignal() {
        // Arrange
        PersonaLineageSnapshot snapshot = new PersonaLineageSnapshot(
                new ParentPersona(List.of("brave"), "characterSketch"), ParentPersona.unknown());

        // Act, Assert
        assertTrue(snapshot.hasAnySignal());
    }

    @Test
    void hasAnySignal_trueWhenOnlyParentBHasSignal() {
        // Arrange
        PersonaLineageSnapshot snapshot = new PersonaLineageSnapshot(
                ParentPersona.unknown(), new ParentPersona(List.of("kind"), "characterSketch"));

        // Act, Assert
        assertTrue(snapshot.hasAnySignal());
    }

    @Test
    void hasAnySignal_falseWhenNeitherParentHasSignal() {
        // Arrange
        PersonaLineageSnapshot snapshot = new PersonaLineageSnapshot(ParentPersona.unknown(), ParentPersona.unknown());

        // Act, Assert
        assertFalse(snapshot.hasAnySignal());
    }

}
