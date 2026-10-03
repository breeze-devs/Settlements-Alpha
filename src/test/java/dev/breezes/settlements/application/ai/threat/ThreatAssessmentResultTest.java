package dev.breezes.settlements.application.ai.threat;

import dev.breezes.settlements.domain.ai.threat.ThreatVerdict;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ThreatAssessmentResultTest {

    private static final CombatSelection ANY_SELECTION = new CombatSelection(FakeCombatOptions.withOrder(0), null);

    @Test
    void construct_combatWithoutASelection_isRejected() {
        // Arrange, Act & Assert -- counterexample: a COMBAT verdict reaching its readers with no option
        // to fight with.
        assertThrows(IllegalArgumentException.class, () -> new ThreatAssessmentResult(ThreatVerdict.COMBAT, null));
    }

    @ParameterizedTest
    @EnumSource(value = ThreatVerdict.class, names = "COMBAT", mode = EnumSource.Mode.EXCLUDE)
    void construct_anyOtherVerdictWithASelection_isRejected(ThreatVerdict verdict) {
        // Arrange, Act & Assert -- counterexample: a PANIC or HOLD result keeping a selection that a
        // later assessment could resume as if the engagement never ended.
        assertThrows(IllegalArgumentException.class, () -> new ThreatAssessmentResult(verdict, ANY_SELECTION));
    }

}
