package dev.breezes.settlements.domain.ai.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BehaviorKeyTest {

    @Test
    void displayNameKey_usesBehaviorTranslationPrefixAndId() {
        // Arrange
        BehaviorKey behaviorKey = BehaviorKey.HARVEST_ORE;

        // Act
        String displayNameKey = behaviorKey.displayNameKey();

        // Assert
        assertEquals("ui.settlements.behavior.behavior.harvest_ore", displayNameKey);
    }

}
