package dev.breezes.settlements.application.ai.dialogue;

import dagger.Lazy;
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
        // Act
        DialogueProvider provider = createProvider("SCRIPTED", true);

        // Assert
        assertInstanceOf(ScriptedDialogueProvider.class, provider);
        assertTrue(provider.isEnabled());
    }

    @Test
    void create_rehearsedMode_returnsRehearsedProvider() {
        // Act
        DialogueProvider provider = createProvider("REHEARSED", true);

        // Assert — REHEARSED now returns the real rehearsed provider with a scripted fallback rung
        assertInstanceOf(RehearsedDialogueProvider.class, provider);
        assertTrue(provider.isEnabled());
    }

    @Test
    void create_rehearsedMode_providerSupportsSweep() {
        // Act
        DialogueProvider provider = createProvider("REHEARSED", true);

        // Assert — sweep must be enabled for the evening event to dispatch
        assertTrue(provider.supportsRehearsedDialogSweep());
    }

    @Test
    void create_unknownModeString_defaultsToScripted() {
        // Act — a typo'd mode must not crash; resolvedMode() falls back to SCRIPTED
        DialogueProvider provider = createProvider("garbage", true);

        // Assert
        assertInstanceOf(ScriptedDialogueProvider.class, provider);
        assertTrue(provider.isEnabled());
    }

    @Test
    void create_inferenceDisabled_forcesScriptedEvenWhenModeIsRehearsed() {
        // Act — the central kill-switch subsumes the configured mode
        DialogueProvider provider = createProvider("REHEARSED", false);

        // Assert — REHEARSED is ignored; the scripted floor is used and never touches the monologue Lazy
        assertInstanceOf(ScriptedDialogueProvider.class, provider);
    }

    private DialogueProvider createProvider(String mode, boolean inferenceEnabled) {
        DialogueConfig dialogueConfig = new DialogueConfig(true, 120);
        RehearsedDialogueConfig rehearsedConfig = new RehearsedDialogueConfig(mode, 12, 30);
        Lazy<MonologueRequestService> lazyMonologue = () -> monologueRequestService;
        return DialogueProviderFactory.create(dialogueConfig, rehearsedConfig, new DialogueLineIndex(), lazyMonologue, inferenceEnabled);
    }

}
