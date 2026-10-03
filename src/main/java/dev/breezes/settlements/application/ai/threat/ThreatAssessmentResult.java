package dev.breezes.settlements.application.ai.threat;

import dev.breezes.settlements.domain.ai.threat.ThreatVerdict;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * One threat assessment's outcome.
 *
 * @param verdict   the villager's response to its current situation
 * @param selection the option and target authorizing the verdict; present exactly when the verdict is COMBAT
 */
public record ThreatAssessmentResult(@Nonnull ThreatVerdict verdict,
                                     @Nullable CombatSelection selection) {

    /**
     * @throws IllegalArgumentException if a selection is absent for COMBAT or present for any other verdict
     */
    public ThreatAssessmentResult {
        Validate.isTrue((verdict == ThreatVerdict.COMBAT) == (selection != null),
                "selection must be present exactly when verdict is COMBAT, but verdict is %s and selection is %s",
                verdict, selection);
    }

}
