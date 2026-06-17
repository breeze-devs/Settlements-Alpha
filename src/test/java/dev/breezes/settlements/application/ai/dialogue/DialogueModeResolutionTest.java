package dev.breezes.settlements.application.ai.dialogue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link DialogueConfig#resolvedMode()} — enum parsing from the config string.
 * No Minecraft types; pure domain logic.
 */
class DialogueModeResolutionTest {

    @Test
    void resolvedMode_returnsOffForDefaultValue() {
        // Arrange — the default config string per @BehaviorConfig annotation
        DialogueConfig config = configWithMode("OFF");

        // Act / Assert
        assertEquals(DialogueMode.OFF, config.resolvedMode());
    }

    @Test
    void resolvedMode_returnsPacks() {
        // Arrange
        DialogueConfig config = configWithMode("PACKS");

        // Act / Assert
        assertEquals(DialogueMode.PACKS, config.resolvedMode());
    }

    @Test
    void resolvedMode_returnsLive() {
        // Arrange
        DialogueConfig config = configWithMode("LIVE");

        // Act / Assert
        assertEquals(DialogueMode.LIVE, config.resolvedMode());
    }

    @Test
    void resolvedMode_isCaseInsensitive() {
        // Arrange — TOML authors might write "packs" or "Packs"
        DialogueConfig config = configWithMode("packs");

        // Act / Assert
        assertEquals(DialogueMode.PACKS, config.resolvedMode());
    }

    @Test
    void resolvedMode_defaultsToOffForUnknownString() {
        // Arrange — typo in the config
        DialogueConfig config = configWithMode("UNKOWN_MODE");

        // Act / Assert — graceful degradation rather than crash
        assertEquals(DialogueMode.OFF, config.resolvedMode());
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static DialogueConfig configWithMode(String mode) {
        // Construct the record with only the mode field meaningful; use defaults for the rest.
        return new DialogueConfig(
                mode,
                "",          // endpointBaseUrl
                "gemma2:2b", // model
                "",          // apiKey
                0.8,         // temperature
                48,          // maxOutputTokens
                120,         // bubbleCharCap
                2,           // maxConcurrentRequests
                1500,        // liveDeadlineMillisAmbient
                3500,        // liveDeadlineMillisPlayer
                12,          // packLinesPerVillager
                30           // packSweepDeadlineSeconds
        );
    }

}
