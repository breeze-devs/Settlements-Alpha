package dev.breezes.settlements.domain.ai.override;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the pure admission and ranking rules the override arbiter applies.
 */
class OverrideArbitrationRulesTest {

    @Test
    void isAdmittedDespiteProtectedPlan_blocksReactiveRequestsAgainstANonInterruptiblePlan() {
        // Counterexample: a trade invite preempting a non-interruptible plan behavior mid-craft.
        assertFalse(OverrideArbitrationRules.isAdmittedDespiteProtectedPlan(OverrideTier.REACTIVE, false));
    }

    @Test
    void isAdmittedDespiteProtectedPlan_blocksOpportunisticRequestsAgainstANonInterruptiblePlan() {
        assertFalse(OverrideArbitrationRules.isAdmittedDespiteProtectedPlan(OverrideTier.OPPORTUNISTIC, false));
    }

    @Test
    void isAdmittedDespiteProtectedPlan_emergencyIgnoresPlanProtection() {
        // Counterexample: a future combat emergency unable to interrupt a villager mid-anvil-repair
        // because that behavior is marked non-interruptible.
        assertTrue(OverrideArbitrationRules.isAdmittedDespiteProtectedPlan(OverrideTier.EMERGENCY, false));
    }

    @Test
    void isAdmittedDespiteProtectedPlan_admitsEveryTierAgainstAnInterruptiblePlan() {
        assertTrue(OverrideArbitrationRules.isAdmittedDespiteProtectedPlan(OverrideTier.REACTIVE, true));
        assertTrue(OverrideArbitrationRules.isAdmittedDespiteProtectedPlan(OverrideTier.OPPORTUNISTIC, true));
    }

    @Test
    void firstOutOfTierOrder_returnsEmptyWhenTiersAreRankedHighestFirst() {
        // Counterexample: two policies sharing a tier flagged as out of order, leaving no way to rank within a tier.
        List<OverrideTier> rankedTiers = List.of(
                OverrideTier.EMERGENCY, OverrideTier.REACTIVE, OverrideTier.REACTIVE, OverrideTier.OPPORTUNISTIC);

        assertTrue(OverrideArbitrationRules.firstOutOfTierOrder(rankedTiers).isEmpty());
    }

    @Test
    void firstOutOfTierOrder_returnsTheIndexOfATierRankedBehindALowerOne() {
        // Counterexample: an opportunistic pickup ranked ahead of a trade invite goes unnoticed, so the pickup is
        // asked first and wins whenever both fire.
        List<OverrideTier> rankedTiers = List.of(OverrideTier.EMERGENCY, OverrideTier.OPPORTUNISTIC, OverrideTier.REACTIVE);

        OptionalInt outOfOrder = OverrideArbitrationRules.firstOutOfTierOrder(rankedTiers);

        assertEquals(OptionalInt.of(2), outOfOrder);
    }

}
