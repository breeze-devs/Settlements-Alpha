package dev.breezes.settlements.application.ai.threat;

import dev.breezes.settlements.domain.ai.perception.PerceivedEntities;
import dev.breezes.settlements.domain.ai.threat.QualifyingHit;
import dev.breezes.settlements.domain.ai.threat.ThreatDecision;
import dev.breezes.settlements.domain.ai.threat.ThreatVerdict;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.Tickable;
import lombok.AccessLevel;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Transient evidence and assessment bookkeeping for one villager.
 * <p>
 * Received hits are retained independently of perception, so a refreshed scan cannot erase evidence
 * of an attacker that is out of sight.
 */
public final class ThreatAssessmentState implements IThreatResponse {

    /**
     * Periodic assessment cadence. Independent of any sensor's own cadence, so this class never
     * couples to how often perception itself refreshes.
     */
    static final ClockTicks ASSESSMENT_INTERVAL = ClockTicks.of(5);

    /**
     * Null until a hit qualifies; expiry does not clear this reference.
     */
    @Getter(AccessLevel.PACKAGE)
    @Nullable
    private QualifyingHit latestHit;

    @Getter(AccessLevel.PACKAGE)
    private boolean assessmentDirty;

    /**
     * Null before this villager's first assessment.
     */
    @Getter(AccessLevel.PACKAGE)
    @Nullable
    private ThreatAssessmentResult latestResult;

    /**
     * Game time of the latest assessment whose combat selection had a target. Every fight enters with one, so this is
     * meaningful whenever the latest verdict is COMBAT.
     */
    @Getter(AccessLevel.PACKAGE)
    private long lastEngagedGameTime;

    /**
     * Game time of the latest assessment that decided PANIC. Every panic enters through that decision, so this is
     * meaningful whenever the latest verdict is PANIC.
     */
    @Getter(AccessLevel.PACKAGE)
    private long lastAlarmedGameTime;

    private PerceivedEntities lastAssessedHostiles = PerceivedEntities.empty();

    private final Tickable cadence = Tickable.staggered(ASSESSMENT_INTERVAL);

    @Override
    public Optional<QualifyingHit> recordHit(@Nullable UUID attackerId, float healthLost, long gameTime) {
        if (attackerId == null || healthLost <= 0f) {
            return Optional.empty();
        }

        QualifyingHit hit = new QualifyingHit(healthLost, gameTime, attackerId);
        this.latestHit = hit;
        this.assessmentDirty = true;
        return Optional.of(hit);
    }

    @Override
    @Nullable
    public ThreatVerdict verdict() {
        return this.latestResult == null ? null : this.latestResult.verdict();
    }

    @Override
    @Nullable
    public CombatSelection selection() {
        return this.latestResult == null ? null : this.latestResult.selection();
    }

    /**
     * Whether a full assessment should run this tick.
     * <p>
     * Should be called per tick: each call advances the cadence by one tick, and whichever reason answers true
     * restarts it.
     *
     * @param currentHostiles this tick's NEARBY_HOSTILES value
     * @return true when an assessment is due
     */
    boolean isDue(@Nonnull PerceivedEntities currentHostiles) {
        boolean cadenceElapsed = this.cadence.tickCheckAndReset(1);
        boolean due = cadenceElapsed || this.assessmentDirty || !Objects.equals(currentHostiles, this.lastAssessedHostiles);
        if (due && !cadenceElapsed) {
            // The sensor refreshes on its own phase, so without the restart a villager with hostiles
            // near would assess on both phases, at up to twice the intended rate.
            this.cadence.reset();
        }
        return due;
    }

    /**
     * Records a completed assessment's outcome, clearing the dirty flag so a hit recorded before this
     * assessment ran does not force another one immediately.
     *
     * @param decision         the response rule that produced the result
     * @param result           the assessment's outcome
     * @param assessedHostiles the NEARBY_HOSTILES value this assessment read, remembered so a later
     *                         unchanged read does not re-trigger {@link #isDue}
     * @param gameTime         the instant the assessment ran
     */
    void recordAssessment(@Nonnull ThreatDecision decision,
                          @Nonnull ThreatAssessmentResult result,
                          @Nonnull PerceivedEntities assessedHostiles,
                          long gameTime) {
        this.latestResult = result;
        this.lastAssessedHostiles = assessedHostiles;
        this.assessmentDirty = false;

        CombatSelection selection = result.selection();
        if (selection != null && selection.targetId() != null) {
            this.lastEngagedGameTime = gameTime;
        }
        // A lingering panic must not restart its own clock, or a hostile loitering in awareness would hold it forever
        if (decision == ThreatDecision.PANIC) {
            this.lastAlarmedGameTime = gameTime;
        }
    }

}
