package dev.breezes.settlements.application.ai.behavior.usecases.villager.cartographer;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChartRaidersConfigTest {

    private static final int ANY_CADENCE_SECONDS = 30;
    private static final int ANY_EXPERIENCE_REWARD = 1;

    @Test
    void rejects_aLevelThatChartsNoRaider() {
        // Counterexample: the validation dropped, so a zero loads and every pulse at that level sketches for nothing.
        Map<String, Integer> raidersPerPulse = Map.of("novice", 2, "apprentice", 0);

        assertThrows(IllegalArgumentException.class,
                () -> new ChartRaidersConfig(ANY_CADENCE_SECONDS, raidersPerPulse, ANY_EXPERIENCE_REWARD));
    }

    @Test
    void accepts_aLevelThatChartsOneRaider() {
        // Counterexample: the bound set one too high, so a pack that charts a single raider at a level fails to load.
        Map<String, Integer> raidersPerPulse = Map.of("novice", 1);

        assertDoesNotThrow(() -> new ChartRaidersConfig(ANY_CADENCE_SECONDS, raidersPerPulse, ANY_EXPERIENCE_REWARD));
    }

}
