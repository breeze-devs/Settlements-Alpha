package dev.breezes.settlements.application.ai.socialcue;

import dev.breezes.settlements.application.ai.dialogue.Occasion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OccasionCadenceProfileTest {

    @Test
    void factorFor_knownOccasions_returnsProfileValues() {
        // Arrange / Act / Assert
        assertEquals(0.6, OccasionCadenceProfile.factorFor(Occasion.MEET));
        assertEquals(1.0, OccasionCadenceProfile.factorFor(Occasion.IDLE));
        assertEquals(0.8, OccasionCadenceProfile.factorFor(Occasion.REST_DAY));
        assertEquals(1.3, OccasionCadenceProfile.factorFor(Occasion.WORK));
        assertEquals(2.0, OccasionCadenceProfile.factorFor(Occasion.PANIC));
        assertEquals(2.0, OccasionCadenceProfile.factorFor(Occasion.PRE_RAID));
        assertEquals(2.0, OccasionCadenceProfile.factorFor(Occasion.RAID));
    }

    @Test
    void factorFor_unprofiledOccasion_returnsDefaultFactor() {
        // Arrange / Act
        double result = OccasionCadenceProfile.factorFor(Occasion.ZOMBIE_SIGHTED);

        // Assert
        assertEquals(OccasionCadenceProfile.DEFAULT_FACTOR, result);
    }

}
