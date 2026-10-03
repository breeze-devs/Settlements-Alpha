package dev.breezes.settlements.application.ai.threat;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CombatOptionCatalogTest {

    @Test
    void constructor_rejectsTwoOptionsDeclaringTheSameOrder() {
        // Arrange -- counterexample: two options both claim order 0, and nothing catches it, leaving
        // their relative selection precedence to depend on incidental Set iteration order.
        CombatOption first = FakeCombatOptions.withOrder(0);
        CombatOption second = FakeCombatOptions.withOrder(0);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> new CombatOptionCatalog(Set.of(first, second)));
    }

    @Test
    void orderedOptions_sortsByDeclaredOrder_regardlessOfInsertionOrder() {
        // Arrange -- counterexample: a catalog that reports options in incidental Set iteration order
        // instead of declared order, so selection precedence among options varies from run to run.
        CombatOption highestOrder = FakeCombatOptions.withOrder(9);
        CombatOption lowestOrder = FakeCombatOptions.withOrder(0);
        CombatOption middleOrder = FakeCombatOptions.withOrder(4);
        CombatOptionCatalog catalog = new CombatOptionCatalog(Set.of(highestOrder, lowestOrder, middleOrder));

        // Act
        List<CombatOption> ordered = catalog.orderedOptions();

        // Assert
        assertEquals(List.of(lowestOrder, middleOrder, highestOrder), ordered);
    }

}
