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
     * Records a hit against this villager and makes an assessment due at once.
     * A missing attacker or a hit for 0 HP is ignored.
     * An accepted hit replaces the previous one without accumulating damage.
     * <p>
     * Callers must exclude self-inflicted hits and damage types that should not alarm the villager.
     *
     * @param attackerId the attacking entity's id, or null when no attacker is known
     * @param healthLost health actually lost to this hit
     * @param gameTime   world game time when the hit occurred
     * @return the recorded hit, or empty when the hit is ignored
     */
    Optional<QualifyingHit> recordHit(@Nullable UUID attackerId, float healthLost, long gameTime);

    /**
     * The latest assessment's verdict, or null before this villager's first assessment.
     */
    @Nullable
    ThreatVerdict verdict();

    /**
     * The option and target the latest assessment authorized; non-null exactly while the verdict is COMBAT.
     */
    @Nullable
    CombatSelection selection();

}
