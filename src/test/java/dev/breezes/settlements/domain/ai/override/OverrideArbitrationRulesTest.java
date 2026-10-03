package dev.breezes.settlements.domain.ai.override;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the pure precedence rules the override arbiter applies.
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
    void firstDuplicate_returnsEmptyWhenEveryPrecedenceIsUnique() {
        List<OverridePrecedence> precedences = List.of(
                new OverridePrecedence(OverrideTier.REACTIVE, 0),
                new OverridePrecedence(OverrideTier.REACTIVE, 1),
                new OverridePrecedence(OverrideTier.OPPORTUNISTIC, 0));

        assertTrue(OverrideArbitrationRules.firstDuplicate(precedences).isEmpty());
    }

    @Test
    void firstDuplicate_returnsTheRepeatedPrecedence() {
        // Counterexample: two policies both declared REACTIVE order 0 and nothing caught it, leaving
        // their relative evaluation order to depend on incidental Set iteration order.
        OverridePrecedence collision = new OverridePrecedence(OverrideTier.REACTIVE, 0);
        List<OverridePrecedence> precedences = List.of(collision, new OverridePrecedence(OverrideTier.OPPORTUNISTIC, 0), collision);

        Optional<OverridePrecedence> duplicate = OverrideArbitrationRules.firstDuplicate(precedences);

        assertEquals(Optional.of(collision), duplicate);
    }

}
