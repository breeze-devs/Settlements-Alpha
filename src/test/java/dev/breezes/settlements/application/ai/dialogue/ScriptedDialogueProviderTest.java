package dev.breezes.settlements.application.ai.dialogue;

import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptedDialogueProviderTest {

    @Test
    void sampleAmbientLine_returnsTranslatableKeyForProfessionOccasion() {
        // Arrange
        ScriptedDialogueProvider provider = new ScriptedDialogueProvider(indexWithSingleKey(), config(true));
        DialogueContext context = DialogueContext.builder()
                .profession(VillagerProfessionKey.FARMER)
                .occasion(Occasion.WORK)
                .build();

        // Act
        Optional<DialogueLine> line = provider.sampleAmbientLine(UUID.randomUUID(), context);

        // Assert
        assertEquals(new DialogueLine.Translatable("farmer.work", List.of()), line.orElseThrow());
    }

    @Test
    void sampleAmbientLine_usesIndexFallback() {
        // Arrange
        ScriptedDialogueProvider provider = new ScriptedDialogueProvider(indexWithSingleKey(), config(true));
        DialogueContext context = DialogueContext.builder()
                .profession(VillagerProfessionKey.NITWIT)
                .occasion(Occasion.WORK)
                .build();

        // Act
        Optional<DialogueLine> line = provider.sampleAmbientLine(UUID.randomUUID(), context);

        // Assert
        assertEquals(new DialogueLine.Translatable("generic.work", List.of()), line.orElseThrow());
    }

    @Test
    void sampleAmbientLine_returnsEmptyWhenScriptedChatterDisabled() {
        // Arrange
        ScriptedDialogueProvider provider = new ScriptedDialogueProvider(indexWithSingleKey(), config(false));
        DialogueContext context = DialogueContext.builder()
                .profession(VillagerProfessionKey.FARMER)
                .occasion(Occasion.WORK)
                .build();

        // Act / Assert
        assertTrue(provider.sampleAmbientLine(UUID.randomUUID(), context).isEmpty());
    }

    private static DialogueLineIndex indexWithSingleKey() {
        return new DialogueLineIndex(Map.of(
                VillagerProfessionKey.FARMER, Map.of(Occasion.WORK, List.of("farmer.work")),
                DialogueLineIndex.GENERIC, Map.of(
                        Occasion.WORK, List.of("generic.work"),
                        Occasion.IDLE, List.of("generic.idle"))));
    }

    private static DialogueConfig config(boolean scriptedChatter) {
        return new DialogueConfig(
                "SCRIPTED",
                scriptedChatter,
                120,
                12,
                30);
    }

}
