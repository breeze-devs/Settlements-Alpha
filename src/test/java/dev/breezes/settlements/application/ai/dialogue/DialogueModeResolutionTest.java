package dev.breezes.settlements.application.ai.dialogue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link DialogueConfig#resolvedMode()} — enum parsing from the config string.
 * No Minecraft types; pure domain logic.
 */
class DialogueModeResolutionTest {

    @Test
    void resolvedMode_returnsScriptedForDefaultValue() {
        // Arrange — the default config string per @BehaviorConfig annotation
        DialogueConfig config = configWithMode("SCRIPTED");

        // Act / Assert
        assertEquals(DialogueMode.SCRIPTED, config.resolvedMode());
    }

    @Test
    void resolvedMode_returnsRehearsed() {
        // Arrange
        DialogueConfig config = configWithMode("REHEARSED");

        // Act / Assert
        assertEquals(DialogueMode.REHEARSED, config.resolvedMode());
    }

    @Test
    void resolvedMode_isCaseInsensitive() {
        // Arrange — TOML authors might write "rehearsed" or "Rehearsed"
        DialogueConfig config = configWithMode("rehearsed");

        // Act / Assert
        assertEquals(DialogueMode.REHEARSED, config.resolvedMode());
    }

    @Test
    void resolvedMode_defaultsToScriptedForUnknownString() {
        // Arrange — typo in the config
        DialogueConfig config = configWithMode("UNKOWN_MODE");

        // Act / Assert — graceful degradation rather than crash
        assertEquals(DialogueMode.SCRIPTED, config.resolvedMode());
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static DialogueConfig configWithMode(String mode) {
        // Construct the record with only the mode field meaningful; use defaults for the rest.
        return new DialogueConfig(
                mode,
                true,         // scriptedChatter
                120,         // bubbleCharCap
                12,          // packLinesPerVillager
                30           // packSweepDeadlineSeconds
        );
    }

}
