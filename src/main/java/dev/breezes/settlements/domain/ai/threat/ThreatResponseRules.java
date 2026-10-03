package dev.breezes.settlements.domain.ai.threat;

import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.time.ClockTicks;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Scores danger and willingness to fight, then chooses whether to continue combat, enter combat, panic,
 * or hold. The previous verdict is the only response history used to choose thresholds.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ThreatResponseRules {

    /**
     * Distance at which a hostile's contribution to danger reaches zero.
     */
    static final double DANGER_FALLOFF_REACH_BLOCKS = 16.0;

    // When combat is not chosen, a lower panic threshold after PANIC or COMBAT prevents small
    // changes in danger from repeatedly switching the villager between panic and hold.
    static final double DANGER_ALARM_THRESHOLD = 0.5;
    static final double DANGER_CALM_THRESHOLD = 0.35;

    static final double NERVE_WILL_WEIGHT = 1.0;
    static final double NERVE_HEALTH_WEIGHT = 1.0;
    static final double NERVE_HOSTILE_THREAT_WEIGHT = 1.0;

    // Staying in a fight requires less nerve than starting one, so a small loss of confidence
    // does not immediately send a committed fighter running.
    static final double NERVE_ENTRY_THRESHOLD = 1.0;
    static final double NERVE_CONTINUE_THRESHOLD = 0.5;

    /**
     * How long a fight may go without an engageable target before it ends.
     */
    static final ClockTicks TARGETLESS_COMBAT_LIMIT = ClockTicks.seconds(5);

    /**
     * How long a panic outlasts the last alarming assessment while a hostile is still in awareness.
     */
    static final ClockTicks PANIC_LINGER = ClockTicks.seconds(4);

    /**
     * One hostile entity's contribution to a situation's danger.
     *
     * @param distanceBlocks the hostile's distance from the villager, in blocks
     * @param threatWeight   the hostile's per-type danger rating; higher is more dangerous
     */
    public static double dangerContribution(double distanceBlocks, double threatWeight) {
        double distanceWeight = Math.max(0.0, 1.0 - distanceBlocks / DANGER_FALLOFF_REACH_BLOCKS);
        return threatWeight * distanceWeight;
    }

    /**
     * Formula to calculate willingness to fight.
     *
     * @param willGeneValue           the villager's {@link GeneType#WILL} value, in [0, 1]
     * @param healthFraction          current health divided by max health, in [0, 1]
     * @param optionNerveContribution the option's contribution to nerve
     * @param totalHostileThreat      the summed threat weight of every aware hostile, regardless of distance
     */
    public static double nerve(double willGeneValue,
                               double healthFraction,
                               double optionNerveContribution,
                               double totalHostileThreat) {
        // Ignore distance: approaching the same hostiles should not make the villager flee from a fight it was starting
        return NERVE_WILL_WEIGHT * willGeneValue
                + NERVE_HEALTH_WEIGHT * healthFraction
                + optionNerveContribution
                - NERVE_HOSTILE_THREAT_WEIGHT * totalHostileThreat;
    }

    /**
     * Applies the response rules in order: continue the current fight, enter a new one, panic, linger in panic, or hold.
     */
    public static ThreatDecision decide(@Nonnull ThreatSituation situation) {
        ThreatVerdict previousVerdict = situation.previousVerdict();
        Double continuingNerve = situation.continuingNerve();
        // A panicked villager must regain enough nerve to enter combat; the lower continuation
        // threshold only applies while it is still fighting. The targetless limit binds even under a
        // live hit, so a fighter struck by an attacker it cannot engage flees instead of standing.
        if (previousVerdict == ThreatVerdict.COMBAT
                && continuingNerve != null
                && (situation.awareHostilePresent() || situation.liveHitPresent())
                && situation.sinceEngageableTarget().getTicks() < TARGETLESS_COMBAT_LIMIT.getTicks()
                && continuingNerve >= NERVE_CONTINUE_THRESHOLD) {
            return ThreatDecision.CONTINUE_COMBAT;
        }

        Double enteringNerve = situation.enteringNerve();
        if (enteringNerve != null && enteringNerve > NERVE_ENTRY_THRESHOLD) {
            return ThreatDecision.ENTER_COMBAT;
        }

        double dangerThreshold = alreadyResponding(previousVerdict) ? DANGER_CALM_THRESHOLD : DANGER_ALARM_THRESHOLD;
        if (situation.danger() >= dangerThreshold || situation.liveHitPresent()) {
            return ThreatDecision.PANIC;
        }

        // A chaser closes the gap between the calm and alarm distances within seconds, so calming the moment the
        // villager crosses it would flip PANIC and HOLD for as long as the chase lasts. Once nothing is in awareness
        // the danger has passed, and the villager calms at once.
        if (previousVerdict == ThreatVerdict.PANIC
                && situation.awareHostilePresent()
                && situation.sinceAlarmed().getTicks() < PANIC_LINGER.getTicks()) {
            return ThreatDecision.LINGER_IN_PANIC;
        }

        return ThreatDecision.HOLD;
    }

    /**
     * Whether the villager was already reacting to danger through combat or panic.
     */
    private static boolean alreadyResponding(@Nullable ThreatVerdict previousVerdict) {
        return previousVerdict == ThreatVerdict.PANIC || previousVerdict == ThreatVerdict.COMBAT;
    }

}
