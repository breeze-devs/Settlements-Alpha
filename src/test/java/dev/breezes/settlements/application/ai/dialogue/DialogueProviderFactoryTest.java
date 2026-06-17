package dev.breezes.settlements.application.ai.dialogue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link DialogueProviderFactory} mode resolution and the OFF fallback.
 * No Minecraft types; pure domain logic.
 */
class DialogueProviderFactoryTest {

    private static final String VALID_ENDPOINT = "http://localhost:11434";

    @Test
    void create_offMode_returnsOffProvider() {
        // Arrange
        DialogueConfig config = config("OFF", "");

        // Act
        DialogueProvider provider = DialogueProviderFactory.create(config);

        // Assert
        assertInstanceOf(OffDialogueProvider.class, provider);
        assertFalse(provider.isEnabled());
    }

    @Test
    void create_packsModeWithBlankEndpoint_fallsBackToOff() {
        // Arrange — misconfigured: PACKS selected but no endpoint set
        DialogueConfig config = config("PACKS", "");

        // Act
        DialogueProvider provider = DialogueProviderFactory.create(config);

        // Assert — must degrade to OFF, not build a client that throws on first call
        assertInstanceOf(OffDialogueProvider.class, provider);
        assertFalse(provider.isEnabled());
    }

    @Test
    void create_liveModeWithBlankEndpoint_fallsBackToOff() {
        // Arrange
        DialogueConfig config = config("LIVE", "");

        // Act
        DialogueProvider provider = DialogueProviderFactory.create(config);

        // Assert
        assertInstanceOf(OffDialogueProvider.class, provider);
        assertFalse(provider.isEnabled());
    }

    @Test
    void create_packsModeWithEndpoint_returnsPacksProvider() {
        // Arrange
        DialogueConfig config = config("PACKS", VALID_ENDPOINT);

        // Act
        DialogueProvider provider = DialogueProviderFactory.create(config);

        // Assert
        assertInstanceOf(PacksDialogueProvider.class, provider);
        assertTrue(provider.isEnabled());
    }

    @Test
    void create_liveModeWithEndpoint_returnsLiveProvider() {
        // Arrange
        DialogueConfig config = config("LIVE", VALID_ENDPOINT);

        // Act
        DialogueProvider provider = DialogueProviderFactory.create(config);

        // Assert
        assertInstanceOf(LiveDialogueProvider.class, provider);
        assertTrue(provider.isEnabled());
    }

    @Test
    void create_unknownModeString_defaultsToOff() {
        // Arrange — a typo'd mode must not crash; resolvedMode() falls back to OFF
        DialogueConfig config = config("garbage", VALID_ENDPOINT);

        // Act
        DialogueProvider provider = DialogueProviderFactory.create(config);

        // Assert
        assertInstanceOf(OffDialogueProvider.class, provider);
        assertFalse(provider.isEnabled());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static DialogueConfig config(String mode, String endpoint) {
        return new DialogueConfig(
                mode,
                endpoint,
                "gemma2:2b",
                "",     // apiKey — none
                0.8,    // temperature
                48,     // maxOutputTokens
                120,    // bubbleCharCap
                2,      // maxConcurrentRequests
                1500,   // liveDeadlineMillisAmbient
                3500,   // liveDeadlineMillisPlayer
                12,     // packLinesPerVillager
                30      // packSweepDeadlineSeconds
        );
    }

}
