package dev.breezes.settlements.application.ai.dialogue;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link DialogueResponseSanitizer}.
 * No Minecraft types; pure domain logic.
 */
class DialogueResponseSanitizerTest {

    private static final int CAP = 120;

    // -------------------------------------------------------------------------
    // Basic sanitization
    // -------------------------------------------------------------------------

    @Test
    void sanitize_returnsEmptyForNull() {
        // Arrange / Act
        Optional<String> result = DialogueResponseSanitizer.sanitize(null, CAP);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void sanitize_returnsEmptyForBlankString() {
        // Arrange / Act
        Optional<String> result = DialogueResponseSanitizer.sanitize("   ", CAP);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void sanitize_trimsWhitespace() {
        // Arrange
        String raw = "  Hello there.  ";

        // Act
        Optional<String> result = DialogueResponseSanitizer.sanitize(raw, CAP);

        // Assert
        assertEquals(Optional.of("Hello there."), result);
    }

    // -------------------------------------------------------------------------
    // Quote stripping
    // -------------------------------------------------------------------------

    @Test
    void sanitize_stripsWrappingDoubleQuotes() {
        // Arrange
        String raw = "\"Good morning!\"";

        // Act
        Optional<String> result = DialogueResponseSanitizer.sanitize(raw, CAP);

        // Assert
        assertEquals(Optional.of("Good morning!"), result);
    }

    @Test
    void sanitize_stripsWrappingSingleQuotes() {
        // Arrange
        String raw = "'Another fine day.'";

        // Act
        Optional<String> result = DialogueResponseSanitizer.sanitize(raw, CAP);

        // Assert
        assertEquals(Optional.of("Another fine day."), result);
    }

    @Test
    void sanitize_stripsWrappingTypographicQuotes() {
        // Arrange — curly double quotes as used by some LLM fine-tunes
        String raw = "“Ripe melons out west!”";

        // Act
        Optional<String> result = DialogueResponseSanitizer.sanitize(raw, CAP);

        // Assert
        assertEquals(Optional.of("Ripe melons out west!"), result);
    }

    @Test
    void sanitize_doesNotStripMismatchedQuotes() {
        // Arrange — mismatched opening/closing should not be stripped
        String raw = "\"Looks good.'";

        // Act
        Optional<String> result = DialogueResponseSanitizer.sanitize(raw, CAP);

        // Assert — left as-is after trim
        assertTrue(result.isPresent());
        assertEquals("\"Looks good.'", result.get());
    }

    // -------------------------------------------------------------------------
    // Newline collapsing
    // -------------------------------------------------------------------------

    @Test
    void sanitize_collapsesNewlinesToSpace() {
        // Arrange — backend ignored the stop:["\n"] sequence
        String raw = "Line one.\nLine two.";

        // Act
        Optional<String> result = DialogueResponseSanitizer.sanitize(raw, CAP);

        // Assert — both lines merged; stop sequence prevents this in prod but we guard here
        assertEquals(Optional.of("Line one. Line two."), result);
    }

    // -------------------------------------------------------------------------
    // Minecraft formatting code stripping
    // -------------------------------------------------------------------------

    @Test
    void sanitize_stripsMinecraftFormattingCodes() {
        // Arrange
        String raw = "§6Golden §rtext";

        // Act
        Optional<String> result = DialogueResponseSanitizer.sanitize(raw, CAP);

        // Assert
        assertEquals(Optional.of("Golden text"), result);
    }

    // -------------------------------------------------------------------------
    // Character cap
    // -------------------------------------------------------------------------

    @Test
    void sanitize_clampsToCharCap() {
        // Arrange — 10 chars of "a" repeated
        String raw = "a".repeat(200);

        // Act
        Optional<String> result = DialogueResponseSanitizer.sanitize(raw, 50);

        // Assert
        assertTrue(result.isPresent());
        assertTrue(result.get().length() <= 50);
    }

    @Test
    void sanitize_returnsFullStringWhenUnderCap() {
        // Arrange
        String raw = "Short line.";

        // Act
        Optional<String> result = DialogueResponseSanitizer.sanitize(raw, CAP);

        // Assert
        assertEquals(Optional.of("Short line."), result);
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test
    void sanitize_returnsEmptyAfterStrippingLeadsToBlank() {
        // Arrange — only a formatting code
        String raw = "§r";

        // Act
        Optional<String> result = DialogueResponseSanitizer.sanitize(raw, CAP);

        // Assert
        assertTrue(result.isEmpty());
    }

}
