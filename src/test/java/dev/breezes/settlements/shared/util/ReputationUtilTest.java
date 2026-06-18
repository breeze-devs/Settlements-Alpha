package dev.breezes.settlements.shared.util;

import dev.breezes.settlements.domain.ai.credibility.CredibilityStore;
import dev.breezes.settlements.domain.ai.eventlane.EventLaneConfig;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReputationUtilTest {

    @Test
    void getOrCreateStore_usesConfiguredCredibilityDecay() {
        // Arrange
        EventLaneConfig config = new EventLaneConfig(100, 50, 200, 25,
                120, 120, 10, 300, 4.0, 0.5, 0.25, "exponential", 0.25f);
        ReputationUtil reputationUtil = new ReputationUtil(config);
        UUID observerId = UUID.randomUUID();

        // Act
        CredibilityStore store = reputationUtil.getOrCreateStore(observerId);

        // Assert
        assertEquals(0.25f, store.decayPerTick(), 0.001f);
    }

}
