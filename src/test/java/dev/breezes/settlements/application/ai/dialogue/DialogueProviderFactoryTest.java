package dev.breezes.settlements.application.ai.dialogue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link DialogueProviderFactory} mode resolution and fallback wiring.
 * No Minecraft types; pure domain logic.
 */
@ExtendWith(MockitoExtension.class)
class DialogueProviderFactoryTest {

    @Mock
    private MonologueRequestService monologueRequestService;

    @Test
    void create_scriptedMode_returnsScriptedProvider() {
        // Arrange
        DialogueConfig config = config("SCRIPTED", true);

        // Act
        DialogueProvider provider = DialogueProviderFactory.create(config, new DialogueLineIndex(), monologueRequestService);

        // Assert
        assertInstanceOf(ScriptedDialogueProvider.class, provider);
        assertTrue(provider.isEnabled());
    }

    @Test
    void create_rehearsedMode_returnsRehearsedProvider() {
        // Arrange
        DialogueConfig config = config("REHEARSED", true);

        // Act
        DialogueProvider provider = DialogueProviderFactory.create(config, new DialogueLineIndex(), monologueRequestService);

        // Assert — REHEARSED now returns the real rehearsed provider with a scripted fallback rung
        assertInstanceOf(RehearsedDialogueProvider.class, provider);
        assertTrue(provider.isEnabled());
    }

    @Test
    void create_rehearsedMode_providerSupportsSweep() {
        // Arrange
        DialogueConfig config = config("REHEARSED", true);

        // Act
        DialogueProvider provider = DialogueProviderFactory.create(config, new DialogueLineIndex(), monologueRequestService);

        // Assert — sweep must be enabled for the evening event to dispatch
        assertTrue(provider.supportsRehearsedDialogSweep());
    }

    @Test
    void create_unknownModeString_defaultsToScripted() {
        // Arrange — a typo'd mode must not crash; resolvedMode() falls back to SCRIPTED
        DialogueConfig config = config("garbage", true);

        // Act
        DialogueProvider provider = DialogueProviderFactory.create(config, new DialogueLineIndex(), monologueRequestService);

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
