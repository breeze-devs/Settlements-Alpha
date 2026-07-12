package dev.breezes.settlements.domain.ai.planning;

import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanIntentTest {

    @Test
    void empty_hasNoSelectionsOrPinsAndIsEmpty() {
        // Arrange, Act
        PlanIntent intent = PlanIntent.empty();

        // Assert
        assertTrue(intent.selections().isEmpty());
        assertTrue(intent.pins().isEmpty());
        assertTrue(intent.isEmpty());
    }

    @Test
    void ofCarriedPins_populatesOnlyPinsAndIsNotEmpty() {
        // Arrange
        PinnedSelection pin = new PinnedSelection(BehaviorKey.TRADE_INITIATE, 4_000, false);

        // Act
        PlanIntent intent = PlanIntent.ofCarriedPins(List.of(pin));

        // Assert — carried pins never populate selections(): they re-enter composition only
        // through the anchor stage, never the LLM-overlay per-band selection contract.
        assertEquals(Map.of(), intent.selections());
        assertEquals(List.of(pin), intent.pins());
        assertFalse(intent.isEmpty());
    }

}
