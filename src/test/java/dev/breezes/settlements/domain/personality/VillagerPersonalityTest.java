package dev.breezes.settlements.domain.personality;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillagerPersonalityTest {

    @Test
    void pending_hasPendingStatusAndEmptyContent() {
        // Arrange, Act
        VillagerPersonality personality = VillagerPersonality.pending();

        // Assert
        assertEquals(PersonalityStatus.PENDING, personality.status());
        assertTrue(personality.adjectives().isEmpty());
        assertEquals("", personality.characterSketch());
        assertEquals("", personality.speechStyle());
    }

    @Test
    void failed_hasFailedStatusAndEmptyContent() {
        // Arrange, Act
        VillagerPersonality personality = VillagerPersonality.failed();

        // Assert
        assertEquals(PersonalityStatus.FAILED, personality.status());
        assertTrue(personality.adjectives().isEmpty());
        assertEquals("", personality.characterSketch());
        assertEquals("", personality.speechStyle());
    }

    @Test
    void ready_carriesTheGivenContent() {
        // Arrange, Act
        VillagerPersonality personality = VillagerPersonality.ready(List.of("gruff", "loyal"), "A gruff blacksmith.", "terse");

        // Assert
        assertEquals(PersonalityStatus.READY, personality.status());
        assertEquals(List.of("gruff", "loyal"), personality.adjectives());
        assertEquals("A gruff blacksmith.", personality.characterSketch());
        assertEquals("terse", personality.speechStyle());
    }

    @Test
    void ready_defensivelyCopiesTheAdjectivesList() {
        // Arrange — a caller-owned mutable list must not leak into stored state.
        List<String> source = new ArrayList<>(List.of("brave", "kind"));
        VillagerPersonality personality = VillagerPersonality.ready(source, "characterSketch", "style");

        // Act
        source.add("mutated");
        source.set(0, "tampered");

        // Assert
        assertEquals(List.of("brave", "kind"), personality.adjectives());
    }

}
