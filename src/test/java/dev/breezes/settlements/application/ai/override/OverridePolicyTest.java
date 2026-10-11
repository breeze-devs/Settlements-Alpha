package dev.breezes.settlements.application.ai.override;

import dev.breezes.settlements.application.ai.courtship.CourtshipSessionRegistry;
import dev.breezes.settlements.application.ai.trading.TradeSessionRegistry;
import dev.breezes.settlements.domain.ai.override.OverrideArbitrationRules;
import dev.breezes.settlements.domain.ai.override.OverrideTier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class OverridePolicyTest {

    @Mock
    private CourtshipSessionRegistry courtshipRegistry;

    @Mock
    private TradeSessionRegistry tradeRegistry;

    @Test
    void combat_isAdmittedDespiteAProtectedPlan() {
        // Counterexample: combat refused while non-interruptible day-plan work runs, so a protected chore holds off a
        // fight.
        OverrideTier combat = new CombatOverridePolicy().tier();

        assertTrue(OverrideArbitrationRules.isAdmittedDespiteProtectedPlan(combat, false));
    }

    @Test
    void combat_preemptsARunningCourtshipOrTrade() {
        // Counterexample: combat sharing the invites' tier, so a villager mid-trade keeps trading while it is attacked.
        OverrideTier combat = new CombatOverridePolicy().tier();
        OverrideTier courtship = new CourtshipAcceptOverridePolicy(this.courtshipRegistry).tier();
        OverrideTier trade = new TradeAcceptOverridePolicy(this.tradeRegistry).tier();

        assertTrue(combat.isAbove(courtship));
        assertTrue(combat.isAbove(trade));
    }

    @Test
    void courtshipAccept_isRefusedAgainstAProtectedPlan() {
        // Counterexample: a courtship invite pulls a villager out of a non-interruptible chore mid-craft.
        OverrideTier courtship = new CourtshipAcceptOverridePolicy(this.courtshipRegistry).tier();

        assertFalse(OverrideArbitrationRules.isAdmittedDespiteProtectedPlan(courtship, false));
    }

    @Test
    void tradeAccept_isRefusedAgainstAProtectedPlan() {
        // Counterexample: a trade invite pulls a villager out of a non-interruptible chore mid-craft.
        OverrideTier trade = new TradeAcceptOverridePolicy(this.tradeRegistry).tier();

        assertFalse(OverrideArbitrationRules.isAdmittedDespiteProtectedPlan(trade, false));
    }

    @Test
    void courtshipAccept_admittedWhenNoActivityIsActive() {
        // With no non-core activity the villager is answering nothing reactive, so the invite must
        // be admitted rather than refused or failing on the absent activity.
        assertTrue(new CourtshipAcceptOverridePolicy(this.courtshipRegistry).isAdmissibleDuring(null));
    }

    @Test
    void tradeAccept_admittedWhenNoActivityIsActive() {
        assertTrue(new TradeAcceptOverridePolicy(this.tradeRegistry).isAdmissibleDuring(null));
    }

}
