package dev.breezes.settlements.application.ai.dialogue;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link UtterancePack}: draw behavior, exhaustion, and uniqueness.
 * No Minecraft types; pure domain logic.
 */
class UtterancePackTest {

    @Test
    void drawLine_returnsLineFromPack() {
        // Arrange
        UtterancePack pack = new UtterancePack(List.of("Hello!", "Goodbye!"), new Random(42));

        // Act
        Optional<String> line = pack.drawLine();

        // Assert
        assertTrue(line.isPresent());
        assertTrue(line.get().equals("Hello!") || line.get().equals("Goodbye!"));
    }

    @Test
    void drawLine_returnsEmptyWhenExhausted() {
        // Arrange
        UtterancePack pack = new UtterancePack(List.of("Only line"), new Random(0));

        // Act — draw the only line, then draw again
        pack.drawLine();
        Optional<String> second = pack.drawLine();

        // Assert
        assertTrue(second.isEmpty());
    }

    @Test
    void drawLine_drainsPack() {
        // Arrange
        List<String> lines = List.of("A", "B", "C", "D", "E");
        UtterancePack pack = new UtterancePack(lines, new Random(0));

        // Act — draw all lines
        Set<String> drawn = new HashSet<>();
        for (int i = 0; i < lines.size(); i++) {
            Optional<String> line = pack.drawLine();
            assertTrue(line.isPresent());
            drawn.add(line.get());
        }

        // Assert — all unique lines were drawn
        assertEquals(new HashSet<>(lines), drawn);
        assertTrue(pack.isEmpty());
    }

    @Test
    void drawLine_doesNotRepeatLines() {
        // Arrange
        List<String> lines = List.of("One", "Two", "Three");
        UtterancePack pack = new UtterancePack(lines, new Random(1));

        // Act
        List<String> drawn = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            pack.drawLine().ifPresent(drawn::add);
        }

        // Assert — no repeats
        assertEquals(lines.size(), new HashSet<>(drawn).size());
    }

    @Test
    void isEmpty_trueForEmptyList() {
        // Arrange
        UtterancePack pack = new UtterancePack(List.of(), new Random(0));

        // Assert
        assertTrue(pack.isEmpty());
        assertEquals(0, pack.size());
    }

    @Test
    void size_decreasesOnDraw() {
        // Arrange
        UtterancePack pack = new UtterancePack(List.of("A", "B", "C"), new Random(0));

        // Act
        assertEquals(3, pack.size());
        pack.drawLine();
        assertEquals(2, pack.size());
        pack.drawLine();
        assertEquals(1, pack.size());
        assertFalse(pack.isEmpty());
        pack.drawLine();
        assertEquals(0, pack.size());
        assertTrue(pack.isEmpty());
    }

}
