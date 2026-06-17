package dev.breezes.settlements.application.ai.dialogue.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link DialogRequest#toJson()}.
 * Verifies the serialized body meets the contract (§3 of llm-contracts.md).
 * No Minecraft types; pure domain logic.
 */
class DialogRequestTest {

    @Test
    void toJson_containsRequiredFields() {
        // Arrange
        DialogRequest request = DialogRequest.builder()
                .model("gemma2:2b")
                .message(LlmMessage.system("You are a villager."))
                .message(LlmMessage.user("Hello!"))
                .maxTokens(48)
                .temperature(0.8)
                .n(1)
                .build();

        // Act
        String json = request.toJson();

        // Assert — all required contract fields are present
        assertTrue(json.contains("\"model\":\"gemma2:2b\""));
        assertTrue(json.contains("\"messages\":["));
        assertTrue(json.contains("\"max_tokens\":48"));
        assertTrue(json.contains("\"temperature\":0.8"));
        assertTrue(json.contains("\"stream\":false"));
        assertTrue(json.contains("\"stop\":[\"\\n\"]"));
    }

    @Test
    void toJson_includesSystemAndUserMessages() {
        // Arrange
        DialogRequest request = DialogRequest.builder()
                .model("gemma2:2b")
                .message(LlmMessage.system("Persona card goes here."))
                .message(LlmMessage.user("What do you think?"))
                .maxTokens(48)
                .temperature(0.8)
                .n(1)
                .build();

        // Act
        String json = request.toJson();

        // Assert
        assertTrue(json.contains("\"role\":\"system\""));
        assertTrue(json.contains("Persona card goes here."));
        assertTrue(json.contains("\"role\":\"user\""));
        assertTrue(json.contains("What do you think?"));
    }

    @Test
    void toJson_escapesSpecialCharactersInContent() {
        // Arrange — content with embedded quotes and backslash
        DialogRequest request = DialogRequest.builder()
                .model("test")
                .message(LlmMessage.user("He said \"hello\" and \\ goodbye."))
                .maxTokens(48)
                .temperature(0.8)
                .n(1)
                .build();

        // Act
        String json = request.toJson();

        // Assert — raw unescaped quotes would break JSON parsing on the backend
        assertFalse(json.contains("\"He said \"hello\""));
        assertTrue(json.contains("\\\"hello\\\""));
        assertTrue(json.contains("\\\\"));
    }

    @Test
    void toJson_setsNForBatchRequest() {
        // Arrange — PACKS sweep uses n > 1
        DialogRequest request = DialogRequest.builder()
                .model("gemma2:2b")
                .message(LlmMessage.system("Persona."))
                .maxTokens(48)
                .temperature(0.8)
                .n(12)
                .build();

        // Act
        String json = request.toJson();

        // Assert
        assertTrue(json.contains("\"n\":12"));
    }

    @Test
    void toJson_omitsAuthorizationHeaderFromBody() {
        // Arrange — apiKey is a header concern, never in the body
        DialogRequest request = DialogRequest.builder()
                .model("gemma2:2b")
                .message(LlmMessage.system("Persona."))
                .maxTokens(48)
                .temperature(0.8)
                .n(1)
                .build();

        // Act
        String json = request.toJson();

        // Assert — no credential material in the body
        assertFalse(json.contains("Authorization"));
        assertFalse(json.contains("Bearer"));
        assertFalse(json.contains("api_key"));
    }

}
