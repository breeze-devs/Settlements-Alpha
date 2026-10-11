package dev.breezes.settlements.domain.ai.override;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.OptionalInt;

/**
 * Override admission rules and policy ranking validation.
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
     * Returns the index of the first tier ranked behind a lower one, or empty if the tiers are ranked
     * highest first.
     */
    public static OptionalInt firstOutOfTierOrder(List<OverrideTier> rankedTiers) {
        for (int i = 1; i < rankedTiers.size(); i++) {
            if (rankedTiers.get(i).isAbove(rankedTiers.get(i - 1))) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }

}
