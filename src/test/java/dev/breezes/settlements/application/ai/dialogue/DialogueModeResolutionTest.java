package dev.breezes.settlements.application.ai.dialogue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link RehearsedDialogueConfig#resolvedMode()} — enum parsing from the config string.
 * No Minecraft types; pure domain logic.
 */
class DialogueModeResolutionTest {

    @Test
    void resolvedMode_returnsScriptedForDefaultValue() {
        // Arrange — the default config string per @BehaviorConfig annotation
        RehearsedDialogueConfig config = configWithMode("SCRIPTED");

        // Act / Assert
        assertEquals(DialogueMode.SCRIPTED, config.resolvedMode());
    }

    @Test
    void resolvedMode_returnsRehearsed() {
        // Arrange
        RehearsedDialogueConfig config = configWithMode("REHEARSED");

        // Act / Assert
        assertEquals(DialogueMode.REHEARSED, config.resolvedMode());
    }

    @Test
    void resolvedMode_isCaseInsensitive() {
        // Arrange — TOML authors might write "rehearsed" or "Rehearsed"
        RehearsedDialogueConfig config = configWithMode("rehearsed");

        // Act / Assert
        assertEquals(DialogueMode.REHEARSED, config.resolvedMode());
    }

    @Test
    void resolvedMode_defaultsToScriptedForUnknownString() {
        // Arrange — typo in the config
        RehearsedDialogueConfig config = configWithMode("UNKOWN_MODE");

        // Act / Assert — graceful degradation rather than crash
        assertEquals(DialogueMode.SCRIPTED, config.resolvedMode());
    }

    private static RehearsedDialogueConfig configWithMode(String mode) {
        // Only the mode field is meaningful here; the sweep-sizing fields use defaults.
        return new RehearsedDialogueConfig(mode, 12, 30);
    }

}
