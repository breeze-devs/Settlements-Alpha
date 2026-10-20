package dev.breezes.settlements.domain.settlement.health;

import dev.breezes.settlements.domain.settlement.health.SettlementHealth.BellPresence;
import dev.breezes.settlements.domain.settlement.health.SettlementHealth.Cause;
import dev.breezes.settlements.domain.settlement.health.SettlementHealth.Status;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SettlementHealthTest {

    /**
     * A minimum residents of the test's own, so a settlement can fall one short of it.
     */
    private static final int MINIMUM = 3;

    @Test
    void isHealthy_withABellAndExactlyTheMinimum() {
        // Act
        SettlementHealth health = new SettlementHealth(MINIMUM, MINIMUM, BellPresence.FOUND);

        // Assert
        assertEquals(Status.HEALTHY, health.status());
        assertEquals(Set.of(), health.causes());
    }

    @Test
    void isUnhealthyForNoBell_whenTheMembersSuffice() {
        // Act
        SettlementHealth health = new SettlementHealth(MINIMUM, MINIMUM, BellPresence.MISSING);

        // Assert
        assertEquals(Status.UNHEALTHY, health.status());
        assertEquals(Set.of(Cause.NO_BELL), health.causes());
    }

    @Test
    void isUnhealthyForTooFewResidents_oneShortOfTheMinimum_evenWithABellFound() {
        // Act
        SettlementHealth health = new SettlementHealth(MINIMUM - 1, MINIMUM, BellPresence.FOUND);

        // Assert
        assertEquals(Status.UNHEALTHY, health.status());
        assertEquals(Set.of(Cause.TOO_FEW_RESIDENTS), health.causes());
    }

    @Test
    void isUnhealthyForBothCauses_inDeclarationOrder_withNoBellAndTooFewResidents() {
        // Act
        SettlementHealth health = new SettlementHealth(MINIMUM - 1, MINIMUM, BellPresence.MISSING);

        // Assert
        assertEquals(Status.UNHEALTHY, health.status());
        assertEquals(List.of(Cause.NO_BELL, Cause.TOO_FEW_RESIDENTS), List.copyOf(health.causes()));
    }

    @Test
    void isUndetermined_whenTheMembersSuffice_andTheBellIsNotKnowable() {
        // Act
        SettlementHealth health = new SettlementHealth(MINIMUM, MINIMUM, BellPresence.NOT_KNOWABLE);

        // Assert
        assertEquals(Status.UNDETERMINED, health.status());
        assertEquals(Set.of(), health.causes());
    }

    @Test
    void isUnhealthyForTooFewResidents_whenTheBellIsNotKnowable() {
        // Act — too few residents decides the health whatever an unloaded chunk might hold
        SettlementHealth health = new SettlementHealth(MINIMUM - 1, MINIMUM, BellPresence.NOT_KNOWABLE);

        // Assert
        assertEquals(Status.UNHEALTHY, health.status());
        assertEquals(Set.of(Cause.TOO_FEW_RESIDENTS), health.causes());
    }

    @Test
    void rejectsANegativeMemberCount() {
        // Act, Assert
        assertThrows(IllegalArgumentException.class, () -> new SettlementHealth(-1, MINIMUM, BellPresence.FOUND));
    }

}
