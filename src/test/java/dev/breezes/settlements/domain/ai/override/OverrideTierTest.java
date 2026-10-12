package dev.breezes.settlements.domain.ai.override;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverrideTierTest {

    @Test
    void outranksReactive_whenEmergency() {
        OverrideTier candidate = OverrideTier.EMERGENCY;
        OverrideTier current = OverrideTier.REACTIVE;

        boolean outranks = candidate.isAbove(current);

        assertTrue(outranks);
    }

    @Test
    void outranksDuty_whenEmergency() {
        // Counterexample: DUTY declared above EMERGENCY, so combat never preempts a running raid duty.
        OverrideTier candidate = OverrideTier.EMERGENCY;
        OverrideTier current = OverrideTier.DUTY;

        boolean outranks = candidate.isAbove(current);

        assertTrue(outranks);
    }

    @Test
    void outranksReactive_whenDuty() {
        // Counterexample: DUTY declared below REACTIVE, so the reactive-activity rule admits a trade invite mid-raid.
        OverrideTier candidate = OverrideTier.DUTY;
        OverrideTier current = OverrideTier.REACTIVE;

        boolean outranks = candidate.isAbove(current);

        assertTrue(outranks);
    }

    @Test
    void outranksOpportunistic_whenReactive() {
        OverrideTier candidate = OverrideTier.REACTIVE;
        OverrideTier current = OverrideTier.OPPORTUNISTIC;

        boolean outranks = candidate.isAbove(current);

        assertTrue(outranks);
    }

    @Test
    void doesNotOutrank_whenTiersAreEqual() {
        OverrideTier candidate = OverrideTier.REACTIVE;
        OverrideTier current = OverrideTier.REACTIVE;

        boolean outranks = candidate.isAbove(current);

        assertFalse(outranks);
    }

    @Test
    void doesNotOutrankReactive_whenOpportunistic() {
        OverrideTier candidate = OverrideTier.OPPORTUNISTIC;
        OverrideTier current = OverrideTier.REACTIVE;

        boolean outranks = candidate.isAbove(current);

        assertFalse(outranks);
    }

}
