package dev.breezes.settlements.application.ai.dialogue.llm;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link LlmResponse} JSON parsing.
 * No Minecraft types; pure domain logic.
 */
class LlmResponseTest {

    // -------------------------------------------------------------------------
    // Basic extraction
    // -------------------------------------------------------------------------

    @Test
    void parse_extractsFirstChoiceContent() {
        // Arrange — minimal valid Chat Completions response
        String json = """
                {
                  "choices": [
                    {
                      "message": {"role": "assistant", "content": "Good morning!"},
                      "finish_reason": "stop"
                    }
                  ]
                }
                """;

        // Act
        LlmResponse response = LlmResponse.parse(json);

        // Assert
        assertEquals(Optional.of("Good morning!"), response.firstContent());
    }

    @Test
    void parse_extractsMultipleChoiceContents() {
        // Arrange — n=3 batch response from the PACKS sweep
        String json = """
                {
                  "choices": [
                    {"message": {"content": "Line one."}, "finish_reason": "stop"},
                    {"message": {"content": "Line two."}, "finish_reason": "stop"},
                    {"message": {"content": "Line three."}, "finish_reason": "stop"}
                  ]
                }
                """;

        // Act
        LlmResponse response = LlmResponse.parse(json);

        // Assert
        List<String> all = response.allContents();
        assertEquals(3, all.size());
        assertEquals("Line one.", all.get(0));
        assertEquals("Line two.", all.get(1));
        assertEquals("Line three.", all.get(2));
    }

    // -------------------------------------------------------------------------
    // Escape handling
    // -------------------------------------------------------------------------

    @Test
    void parse_handlesEscapedQuotesInContent() {
        // Arrange
        String json = "{\"choices\":[{\"message\":{\"content\":\"He said \\\"hello\\\"\"}}]}";

        // Act
        LlmResponse response = LlmResponse.parse(json);

        // Assert
        assertEquals(Optional.of("He said \"hello\""), response.firstContent());
    }

    @Test
    void parse_handlesEscapedNewlineInContent() {
        // Arrange
        String json = "{\"choices\":[{\"message\":{\"content\":\"Line A\\nLine B\"}}]}";

        // Act
        LlmResponse response = LlmResponse.parse(json);

        // Assert — extracted as \n; the sanitizer will collapse it later
        assertEquals(Optional.of("Line A\nLine B"), response.firstContent());
    }

    // -------------------------------------------------------------------------
    // Graceful degradation
    // -------------------------------------------------------------------------

    @Test
    void parse_returnsEmptyForNullInput() {
        // Arrange / Act
        LlmResponse response = LlmResponse.parse(null);

        // Assert
        assertTrue(response.firstContent().isEmpty());
        assertTrue(response.allContents().isEmpty());
    }

    @Test
    void parse_returnsEmptyForMalformedJson() {
        // Arrange / Act
        LlmResponse response = LlmResponse.parse("this is not json at all");

        // Assert
        assertTrue(response.firstContent().isEmpty());
    }

    @Test
    void parse_returnsEmptyForEmptyChoicesArray() {
        // Arrange
        String json = "{\"choices\":[]}";

        // Act
        LlmResponse response = LlmResponse.parse(json);

        // Assert
        assertTrue(response.firstContent().isEmpty());
    }

}
