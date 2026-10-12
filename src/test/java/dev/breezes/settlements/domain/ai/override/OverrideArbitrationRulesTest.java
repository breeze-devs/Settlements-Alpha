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
    void isAdmittedDespiteProtectedPlan_blocksDutyRequestsAgainstANonInterruptiblePlan() {
        // Counterexample: a duty given EMERGENCY's exemption, so it pulls a villager out of a non-interruptible chore
        // in any activity its policy admits.
        assertFalse(OverrideArbitrationRules.isAdmittedDespiteProtectedPlan(OverrideTier.DUTY, false));
    }

    @Test
    void isAdmittedDespiteProtectedPlan_admitsEveryTierAgainstAnInterruptiblePlan() {
        assertTrue(OverrideArbitrationRules.isAdmittedDespiteProtectedPlan(OverrideTier.REACTIVE, true));
        assertTrue(OverrideArbitrationRules.isAdmittedDespiteProtectedPlan(OverrideTier.OPPORTUNISTIC, true));
    }

    @Test
    void isAdmittedDuringActivity_refusesTiersBelowDutyDuringAReactiveActivity() {
        // Counterexample: a trade invite or an item pickup admitted while the villager shelters from a raid.
        assertFalse(OverrideArbitrationRules.isAdmittedDuringActivity(OverrideTier.REACTIVE, true));
        assertFalse(OverrideArbitrationRules.isAdmittedDuringActivity(OverrideTier.OPPORTUNISTIC, true));
    }

    @Test
    void isAdmittedDuringActivity_admitsDutyAndAboveDuringAReactiveActivity() {
        // Counterexample: the boundary set one tier too high, so a raid duty is refused during the raid it serves.
        assertTrue(OverrideArbitrationRules.isAdmittedDuringActivity(OverrideTier.DUTY, true));
        assertTrue(OverrideArbitrationRules.isAdmittedDuringActivity(OverrideTier.EMERGENCY, true));
    }

    @Test
    void isAdmittedDuringActivity_admitsEveryTierDuringANonReactiveActivity() {
        // Counterexample: tiers below DUTY refused regardless of the activity, so no invite or pickup ever runs.
        for (OverrideTier tier : OverrideTier.values()) {
            assertTrue(OverrideArbitrationRules.isAdmittedDuringActivity(tier, false), tier.name());
        }
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
