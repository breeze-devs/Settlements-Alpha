package dev.breezes.settlements.domain.ai.override;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link OverridePrecedence}'s sort order: tier first, then explicit order within
 * that tier.
 */
class OverridePrecedenceTest {

    @Test
    void sorting_ordersByTierBeforeOrder() {
        // Arrange — a REACTIVE entry declared with a low order must still sort behind every
        // EMERGENCY entry, however high that entry's own order is.
        OverridePrecedence reactiveFirst = new OverridePrecedence(OverrideTier.REACTIVE, 0);
        OverridePrecedence emergencyLast = new OverridePrecedence(OverrideTier.EMERGENCY, 99);

        // Act
        List<OverridePrecedence> sorted = List.of(reactiveFirst, emergencyLast).stream().sorted().toList();

        // Assert — a comparator that sorted by order before tier would put reactiveFirst ahead here.
        assertEquals(List.of(emergencyLast, reactiveFirst), sorted);
    }

    @Test
    void sorting_ordersByOrderWithinTheSameTier() {
        // Arrange
        OverridePrecedence trade = new OverridePrecedence(OverrideTier.REACTIVE, 1);
        OverridePrecedence courtship = new OverridePrecedence(OverrideTier.REACTIVE, 0);

        // Act
        List<OverridePrecedence> sorted = List.of(trade, courtship).stream().sorted().toList();

        // Assert — a same-tier collision resolved by declaration order rather than the declared
        // order value would leave courtship second here, not first.
        assertEquals(List.of(courtship, trade), sorted);
    }

}
