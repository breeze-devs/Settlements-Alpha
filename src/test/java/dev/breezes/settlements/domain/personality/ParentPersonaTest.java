package dev.breezes.settlements.domain.personality;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParentPersonaTest {

    @Test
    void constructor_nullAdjectivesAndCharacterSketchCoerceToEmpty() {
        // Arrange, Act
        ParentPersona persona = new ParentPersona(null, null);

        // Assert
        assertTrue(persona.adjectives().isEmpty());
        assertEquals("", persona.characterSketch());
    }

    @Test
    void unknown_hasNoSignal() {
        // Arrange, Act
        ParentPersona persona = ParentPersona.unknown();

        // Assert
        assertFalse(persona.hasSignal());
    }

    @Test
    void hasSignal_trueWhenOnlyAdjectivesPresent() {
        // Arrange, Act
        ParentPersona persona = new ParentPersona(List.of("brave"), "");

        // Assert
        assertTrue(persona.hasSignal());
    }

    @Test
    void hasSignal_trueWhenOnlyCharacterSketchPresent() {
        // Arrange, Act
        ParentPersona persona = new ParentPersona(List.of(), "A quiet farmer.");

        // Assert
        assertTrue(persona.hasSignal());
    }

    @Test
    void hasSignal_falseWhenBothEmpty() {
        // Arrange, Act
        ParentPersona persona = new ParentPersona(List.of(), "");

        // Assert
        assertFalse(persona.hasSignal());
    }

}
