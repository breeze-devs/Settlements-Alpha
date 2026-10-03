package dev.breezes.settlements.domain.ai.override;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Override admission rules and precedence conflict detection.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OverrideArbitrationRules {

    /**
     * Whether the candidate tier passes the day plan's interruption protection: an interruptible
     * plan admits every tier; a protected plan admits only {@link OverrideTier#EMERGENCY}.
     */
    public static boolean isAdmittedDespiteProtectedPlan(OverrideTier tier, boolean planInterruptible) {
        return planInterruptible || tier == OverrideTier.EMERGENCY;
    }

    /**
     * Returns the precedence whose second occurrence appears earliest in the list, or empty if
     * every precedence is unique.
     */
    public static Optional<OverridePrecedence> firstDuplicate(List<OverridePrecedence> precedences) {
        Set<OverridePrecedence> seen = new HashSet<>();
        for (OverridePrecedence precedence : precedences) {
            if (!seen.add(precedence)) {
                return Optional.of(precedence);
            }
        }
        return Optional.empty();
    }

}
