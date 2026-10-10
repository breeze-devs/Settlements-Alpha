package dev.breezes.settlements.application.ai.threat;

import dev.breezes.settlements.domain.ai.threat.QualifyingHit;
import dev.breezes.settlements.domain.ai.threat.ThreatVerdict;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * One villager's response to threats.
 */
public interface IThreatResponse {

    /**
     * Records a hit against this villager and marks its threat assessment as due immediately.
     * Hits with no known attacker or no health loss are ignored.
     * Each accepted hit replaces the previous hit; damage is not accumulated.
     * <p>
     * Callers must exclude self-inflicted hits and damage types that should not alarm the villager.
     *
     * @param attackerId the attacking entity's id, or null when no attacker is known
     * @param healthLost health actually lost to this hit, in HP
     * @param gameTime   world game time when the hit occurred, in ticks
     * @return the recorded hit, or empty when the hit is ignored
     */
    Optional<QualifyingHit> recordHit(@Nullable UUID attackerId, float healthLost, long gameTime);

    /**
     * Returns the latest assessment's verdict, or null before this villager's first assessment.
     */
    @Nullable
    ThreatVerdict verdict();

    /**
     * Returns the combat selection from the latest assessment, or null when the verdict is not COMBAT.
     * After {@link #revokeSelection()}, returns null until the next assessment, even if the verdict is still COMBAT.
     */
    @Nullable
    CombatSelection selection();

    /**
     * Revokes the current combat selection and marks a new assessment as due immediately.
     * The current verdict remains unchanged until the next assessment.
     */
    void revokeSelection();

}
