package dev.breezes.settlements.application.ai.dialogue;

import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogueLineIndexTest {

    @Test
    void resolveKeys_prefersProfessionOccasionOverFallbacks() {
        // Arrange
        DialogueLineIndex index = new DialogueLineIndex(Map.of(
                VillagerProfessionKey.FARMER, Map.of(
                        Occasion.WORK, List.of("farmer.work"),
                        Occasion.IDLE, List.of("farmer.idle")),
                DialogueLineIndex.GENERIC, Map.of(
                        Occasion.WORK, List.of("generic.work"),
                        Occasion.IDLE, List.of("generic.idle"))));

        // Act
        List<String> resolved = index.resolveKeys(VillagerProfessionKey.FARMER, Occasion.WORK).orElseThrow();

        // Assert
        assertEquals(List.of("farmer.work"), resolved);
    }

    @Test
    void resolveKeys_fallsBackToGenericOccasionBeforeProfessionIdle() {
        // Arrange
        DialogueLineIndex index = new DialogueLineIndex(Map.of(
                VillagerProfessionKey.FARMER, Map.of(Occasion.IDLE, List.of("farmer.idle")),
                DialogueLineIndex.GENERIC, Map.of(
                        Occasion.WORK, List.of("generic.work"),
                        Occasion.IDLE, List.of("generic.idle"))));

        // Act
        List<String> resolved = index.resolveKeys(VillagerProfessionKey.FARMER, Occasion.WORK).orElseThrow();

        // Assert
        assertEquals(List.of("generic.work"), resolved);
    }

    @Test
    void resolveKeys_fallsBackToProfessionIdleBeforeGenericIdle() {
        // Arrange
        DialogueLineIndex index = new DialogueLineIndex(Map.of(
                VillagerProfessionKey.FARMER, Map.of(Occasion.IDLE, List.of("farmer.idle")),
                DialogueLineIndex.GENERIC, Map.of(Occasion.IDLE, List.of("generic.idle"))));

        // Act
        List<String> resolved = index.resolveKeys(VillagerProfessionKey.FARMER, Occasion.PANIC).orElseThrow();

        // Assert
        assertEquals(List.of("farmer.idle"), resolved);
    }

    @Test
    void resolveKeys_returnsEmptyWhenNoFallbackExists() {
        // Arrange
        DialogueLineIndex index = new DialogueLineIndex(Map.of());

        // Act / Assert
        assertTrue(index.resolveKeys(VillagerProfessionKey.FARMER, Occasion.WORK).isEmpty());
    }

    @Test
    void resolveKeys_withFacet_combinesBasePoolAndFacetPool() {
        // Arrange — both a base pool and a facet pool exist at the same occasion; facet lines should
        // be appended to the base lines so callers see the full combined set, not just one or the other.
        DialogueLineIndex index = new DialogueLineIndex(
                Map.of(VillagerProfessionKey.FARMER, Map.of(Occasion.IDLE, List.of("farmer.idle"))),
                Map.of(VillagerProfessionKey.FARMER, Map.of(
                        Occasion.IDLE, Map.of(DialogueFacet.WAS_CURED, List.of("farmer.idle.cured")))));

        // Act
        List<String> resolved = index.resolveKeys(VillagerProfessionKey.FARMER, Occasion.IDLE,
                Set.of(DialogueFacet.WAS_CURED)).orElseThrow();

        // Assert
        assertEquals(List.of("farmer.idle", "farmer.idle.cured"), resolved);
    }

    @Test
    void resolveKeys_withFacet_fallsBackToOccasionPoolWhenFacetMissing() {
        // Arrange
        DialogueLineIndex index = new DialogueLineIndex(
                Map.of(DialogueLineIndex.GENERIC, Map.of(Occasion.MORNING, List.of("generic.morning"))),
                Map.of());

        // Act
        List<String> resolved = index.resolveKeys(VillagerProfessionKey.FARMER, Occasion.MORNING,
                Set.of(DialogueFacet.WAS_CURED)).orElseThrow();

        // Assert
        assertEquals(List.of("generic.morning"), resolved);
    }

    @Test
    void resolveKeys_withFacet_doesNotBleedIdleFacetPoolIntoNonIdleOccasion() {
        // Arrange — the facet pool only exists at IDLE; the base pool exists at MORNING.
        // The bug being guarded: the old IDLE fallback inside the facet walk would surface
        // generic.idle.cured lines when a MORNING occasion was requested, because it fell
        // through to IDLE when no facet pool existed at MORNING.
        DialogueLineIndex index = new DialogueLineIndex(
                Map.of(DialogueLineIndex.GENERIC, Map.of(
                        Occasion.MORNING, List.of("generic.morning"),
                        Occasion.IDLE, List.of("generic.idle"))),
                Map.of(DialogueLineIndex.GENERIC, Map.of(
                        Occasion.IDLE, Map.of(DialogueFacet.WAS_CURED, List.of("generic.idle.cured")))));

        // Act
        List<String> resolved = index.resolveKeys(VillagerProfessionKey.FARMER, Occasion.MORNING,
                Set.of(DialogueFacet.WAS_CURED)).orElseThrow();

        // Assert — only the MORNING base pool should appear; IDLE-cured lines must not bleed in
        assertTrue(resolved.contains("generic.morning"));
        assertTrue(resolved.stream().noneMatch(key -> key.contains("idle")));
    }

    @Test
    void defaultIndex_resolvesDiscretePhaseTwoOccasions() {
        // Arrange
        DialogueLineIndex index = new DialogueLineIndex();

        // Act / Assert
        assertTrue(index.resolveKeys(VillagerProfessionKey.FARMER, Occasion.MORNING).orElseThrow()
                .contains("dialogue.settlements.generic.morning.1"));
        assertTrue(index.resolveKeys(VillagerProfessionKey.FARMER, Occasion.EVENING).orElseThrow()
                .contains("dialogue.settlements.generic.evening.1"));
        assertTrue(index.resolveKeys(VillagerProfessionKey.FARMER, Occasion.REST_DAY).orElseThrow()
                .contains("dialogue.settlements.farmer.rest_day.1"));
        assertTrue(index.resolveKeys(VillagerProfessionKey.FARMER, Occasion.ZOMBIE_SIGHTED).orElseThrow()
                .contains("dialogue.settlements.farmer.zombie_sighted.1"));
    }

}
