package dev.breezes.settlements.application.ai.dialogue;

import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link OccasionSetResolver}. No Minecraft types; pure domain logic.
 */
class OccasionSetResolverTest {

    private final OccasionSetResolver resolver = new OccasionSetResolver();

    @Test
    void resolve_normalProfession_containsCoreOccasions() {
        // Arrange
        VillagerProfessionKey farmer = VillagerProfessionKey.FARMER;

        // Act
        Set<Occasion> occasions = resolver.resolve(farmer);

        // Assert — all core occasions must be present for a non-nitwit (CORE = IDLE, WORK, MEET, REST_DAY)
        assertTrue(occasions.contains(Occasion.IDLE));
        assertTrue(occasions.contains(Occasion.WORK));
        assertTrue(occasions.contains(Occasion.MEET));
        assertTrue(occasions.contains(Occasion.REST_DAY));
        // MORNING/EVENING are SCRIPTED_ONLY in v1 — they stay on the floor, never rehearsed
        assertFalse(occasions.contains(Occasion.MORNING));
        assertFalse(occasions.contains(Occasion.EVENING));
    }

    @Test
    void resolve_normalProfession_doesNotContainReactiveOccasions() {
        // Arrange
        VillagerProfessionKey cleric = VillagerProfessionKey.CLERIC;

        // Act
        Set<Occasion> occasions = resolver.resolve(cleric);

        // Assert — ZOMBIE_SIGHTED stays on the SCRIPTED floor; it must not be in the rehearsal set
        assertFalse(occasions.contains(Occasion.ZOMBIE_SIGHTED));
    }

    @Test
    void resolve_nitwit_dropsWork() {
        // Arrange
        VillagerProfessionKey nitwit = VillagerProfessionKey.NITWIT;

        // Act
        Set<Occasion> occasions = resolver.resolve(nitwit);

        // Assert — nitwits have no work window; generating a WORK pack would waste token budget
        assertFalse(occasions.contains(Occasion.WORK));
    }

    @Test
    void resolve_nitwit_keepsCoreNonWorkOccasions() {
        // Arrange
        VillagerProfessionKey nitwit = VillagerProfessionKey.NITWIT;

        // Act
        Set<Occasion> occasions = resolver.resolve(nitwit);

        // Assert — nitwits still rehearse ambient occasions (IDLE, MEET, REST_DAY)
        assertTrue(occasions.contains(Occasion.IDLE));
        assertTrue(occasions.contains(Occasion.MEET));
        assertTrue(occasions.contains(Occasion.REST_DAY));
    }

}
