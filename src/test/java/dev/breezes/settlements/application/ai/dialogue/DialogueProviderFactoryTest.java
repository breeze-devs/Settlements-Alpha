package dev.breezes.settlements.application.ai.dialogue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link DialogueProviderFactory} mode resolution and fallback wiring.
 * No Minecraft types; pure domain logic.
 */
class DialogueProviderFactoryTest {

    @Test
    void create_scriptedMode_returnsScriptedProvider() {
        // Arrange
        DialogueConfig config = config("SCRIPTED", true);

        // Act
        DialogueProvider provider = DialogueProviderFactory.create(config, new DialogueLineIndex());

        // Assert
        assertInstanceOf(ScriptedDialogueProvider.class, provider);
        assertTrue(provider.isEnabled());
    }

    @Test
    void create_rehearsedModeWithBlankEndpoint_fallsBackToScripted() {
        // Arrange — phase 1 keeps REHEARSED on SCRIPTED until MONOLOGUE runtime wiring lands.
        DialogueConfig config = config("REHEARSED", true);

        // Act
        DialogueProvider provider = DialogueProviderFactory.create(config, new DialogueLineIndex());

        // Assert — must degrade to SCRIPTED, not build a client that throws on first call
        assertInstanceOf(ScriptedDialogueProvider.class, provider);
        assertTrue(provider.isEnabled());
    }

    @Test
    void create_rehearsedMode_degradesToScriptedDuringPhaseOne() {
        // Arrange
        DialogueConfig config = config("REHEARSED", true);

        // Act
        DialogueProvider provider = DialogueProviderFactory.create(config, new DialogueLineIndex());

        // Assert
        assertInstanceOf(ScriptedDialogueProvider.class, provider);
        assertTrue(provider.isEnabled());
    }

    @Test
    void create_unknownModeString_defaultsToScripted() {
        // Arrange — a typo'd mode must not crash; resolvedMode() falls back to SCRIPTED
        DialogueConfig config = config("garbage", true);

        // Act
        DialogueProvider provider = DialogueProviderFactory.create(config, new DialogueLineIndex());

        // Assert
        assertInstanceOf(ScriptedDialogueProvider.class, provider);
        assertTrue(provider.isEnabled());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static DialogueConfig config(String mode, boolean scriptedChatter) {
        return new DialogueConfig(
                mode,
                scriptedChatter,
                120,    // bubbleCharCap
                12,     // packLinesPerVillager
                30      // packSweepDeadlineSeconds
        );
    }

}
