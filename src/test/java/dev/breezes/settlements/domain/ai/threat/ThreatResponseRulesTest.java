package dev.breezes.settlements.domain.ai.threat;

import dev.breezes.settlements.domain.genetics.GeneticsProfile;
import dev.breezes.settlements.domain.time.ClockTicks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThreatResponseRulesTest {

    private static final double AVERAGE_WILL = GeneticsProfile.SEED_MEAN;
    private static final double LOW_WILL = GeneticsProfile.SEED_MEAN - 2 * GeneticsProfile.SEED_STDDEV;
    private static final double FULL_HEALTH = 1.0;
    private static final double ORDINARY_STRENGTH = 0.7;
    private static final double ORDINARY_THREAT_WEIGHT = 1.0;
    private static final double CLOSE_DISTANCE_BLOCKS = 2.0;
    private static final double VANILLA_ZOMBIE_PANIC_RANGE_BLOCKS = 8.0;
    private static final ClockTicks JUST_UNDER_TARGETLESS_LIMIT =
            ClockTicks.of(ThreatResponseRules.TARGETLESS_COMBAT_LIMIT.getTicks() - 1);
    private static final ClockTicks JUST_UNDER_PANIC_LINGER = ClockTicks.of(ThreatResponseRules.PANIC_LINGER.getTicks() - 1);

    // --- Calibration scenarios ---

    @Test
    void decide_averageWillFullHealthOneEngageableHostile_entersCombat() {
        // Arrange -- counterexample: a capable, healthy villager with a usable option still refusing
        // to fight a single ordinary hostile.
        ThreatSituation situation = ThreatSituation.builder()
                .danger(ThreatResponseRules.dangerContribution(CLOSE_DISTANCE_BLOCKS, ORDINARY_THREAT_WEIGHT))
                .awareHostilePresent(true)
                .enteringNerve(ThreatResponseRules.nerve(AVERAGE_WILL, FULL_HEALTH, ORDINARY_STRENGTH, ORDINARY_THREAT_WEIGHT))
                .build();

        // Act
        ThreatDecision decision = ThreatResponseRules.decide(situation);

        // Assert
        assertEquals(ThreatDecision.ENTER_COMBAT, decision);
    }

    @Test
    void decide_sameVillagerAgainstAPack_panics() {
        // Arrange -- counterexample: hostile threat weighted so lightly that a pack still leaves the
        // lone-hostile fighter's nerve intact.
        int packSize = 5;
        ThreatSituation situation = ThreatSituation.builder()
                .danger(packSize * ThreatResponseRules.dangerContribution(CLOSE_DISTANCE_BLOCKS, ORDINARY_THREAT_WEIGHT))
                .awareHostilePresent(true)
                .enteringNerve(ThreatResponseRules.nerve(AVERAGE_WILL, FULL_HEALTH, ORDINARY_STRENGTH, packSize * ORDINARY_THREAT_WEIGHT))
                .build();

        // Act
        ThreatDecision decision = ThreatResponseRules.decide(situation);

        // Assert
        assertEquals(ThreatDecision.PANIC, decision);
    }

    @Test
    void decide_lowWillAgainstOneHostile_panics() {
        // Arrange -- counterexample: a nerve formula where WIL's weight is too small to ever flip the
        // outcome, making genetics irrelevant to combat entry.
        ThreatSituation situation = ThreatSituation.builder()
                .danger(ThreatResponseRules.dangerContribution(CLOSE_DISTANCE_BLOCKS, ORDINARY_THREAT_WEIGHT))
                .awareHostilePresent(true)
                .enteringNerve(ThreatResponseRules.nerve(LOW_WILL, FULL_HEALTH, ORDINARY_STRENGTH, ORDINARY_THREAT_WEIGHT))
                .build();

        // Act
        ThreatDecision decision = ThreatResponseRules.decide(situation);

        // Assert
        assertEquals(ThreatDecision.PANIC, decision);
    }

    @Test
    void decide_noOptionAtVanillaZombiePanicRange_alarmsFromHold() {
        // Arrange -- counterexample: an alarm threshold so high (or a falloff so steep) that a hostile
        // well within vanilla's own panic range fails to alarm an unarmed villager.
        ThreatSituation situation = ThreatSituation.builder()
                .previousVerdict(ThreatVerdict.HOLD)
                .danger(ThreatResponseRules.dangerContribution(VANILLA_ZOMBIE_PANIC_RANGE_BLOCKS, ORDINARY_THREAT_WEIGHT))
                .awareHostilePresent(true)
                .build();

        // Act
        ThreatDecision decision = ThreatResponseRules.decide(situation);

        // Assert
        assertEquals(ThreatDecision.PANIC, decision);
    }

    @Test
    void decide_noOptionAQuarterBeyondVanillaZombiePanicRange_staysOnHold() {
        // Arrange -- counterexample: a falloff shallow enough (or an alarm threshold low enough) that a
        // lone hostile alarms a villager well beyond vanilla's range, so "alarms at about vanilla's
        // range" holds only at its lower bound.
        double aQuarterBeyondBlocks = VANILLA_ZOMBIE_PANIC_RANGE_BLOCKS * 1.25;
        ThreatSituation situation = ThreatSituation.builder()
                .previousVerdict(ThreatVerdict.HOLD)
                .danger(ThreatResponseRules.dangerContribution(aQuarterBeyondBlocks, ORDINARY_THREAT_WEIGHT))
                .awareHostilePresent(true)
                .build();

        // Act
        ThreatDecision decision = ThreatResponseRules.decide(situation);

        // Assert
        assertEquals(ThreatDecision.HOLD, decision);
    }

    @Test
    void decide_hostileLoiteringBetweenCalmAndAlarm_previousPanicStaysPanic() {
        // Arrange -- distance derived from the production alarm/calm thresholds and falloff reach so a
        // retune of any of the three re-aims this test instead of breaking it.
        ThreatSituation situation = ThreatSituation.builder()
                .previousVerdict(ThreatVerdict.PANIC)
                .danger(ThreatResponseRules.dangerContribution(midwayBetweenCalmAndAlarmDistance(), ORDINARY_THREAT_WEIGHT))
                .awareHostilePresent(true)
                .build();

        // Act -- counterexample: a PANIC villager using the alarm (not calm) threshold, dropping out of
        // PANIC as soon as danger falls below alarm instead of below the lower calm floor.
        ThreatDecision decision = ThreatResponseRules.decide(situation);

        // Assert
        assertEquals(ThreatDecision.PANIC, decision);
    }

    @Test
    void decide_hostileLoiteringBetweenCalmAndAlarm_previousHoldStaysHold() {
        // Arrange -- same loitering distance as the PANIC case above, from the opposite previous state.
        ThreatSituation situation = ThreatSituation.builder()
                .previousVerdict(ThreatVerdict.HOLD)
                .danger(ThreatResponseRules.dangerContribution(midwayBetweenCalmAndAlarmDistance(), ORDINARY_THREAT_WEIGHT))
                .awareHostilePresent(true)
                .build();

        // Act -- counterexample: a HOLD villager using the calm (not alarm) threshold, alarming at a
        // distance that would only justify staying alarmed, not becoming alarmed in the first place.
        ThreatDecision decision = ThreatResponseRules.decide(situation);

        // Assert
        assertEquals(ThreatDecision.HOLD, decision);
    }

    private static double midwayBetweenCalmAndAlarmDistance() {
        double calmDistance = ThreatResponseRules.DANGER_FALLOFF_REACH_BLOCKS * (1.0 - ThreatResponseRules.DANGER_CALM_THRESHOLD);
        double alarmDistance = ThreatResponseRules.DANGER_FALLOFF_REACH_BLOCKS * (1.0 - ThreatResponseRules.DANGER_ALARM_THRESHOLD);
        return (calmDistance + alarmDistance) / 2.0;
    }

    @Test
    void dangerContribution_beyondTheFalloffReach_isZero() {
        // Arrange -- counterexample: an unclamped falloff going negative past its reach, so a far hostile
        // subtracts from the danger of nearer ones.
        double beyondReachBlocks = ThreatResponseRules.DANGER_FALLOFF_REACH_BLOCKS * 2;

        // Act
        double contribution = ThreatResponseRules.dangerContribution(beyondReachBlocks, ORDINARY_THREAT_WEIGHT);

        // Assert
        assertEquals(0.0, contribution);
    }

    // --- Panic linger ---

    @Test
    void decide_chasedPastTheCalmDistanceWithinThePanicLinger_staysPanicked() {
        // Arrange -- the villager has just outrun its chaser beyond the calm distance.
        ThreatSituation situation = ThreatSituation.builder()
                .previousVerdict(ThreatVerdict.PANIC)
                .danger(ThreatResponseRules.dangerContribution(beyondCalmDistance(), ORDINARY_THREAT_WEIGHT))
                .awareHostilePresent(true)
                .sinceAlarmed(JUST_UNDER_PANIC_LINGER)
                .build();

        // Act -- counterexample: calming the moment danger dips below the calm threshold, so the villager stops,
        // the chaser catches up, and the verdict flips between PANIC and HOLD for as long as the chase lasts.
        ThreatDecision decision = ThreatResponseRules.decide(situation);

        // Assert
        assertEquals(ThreatDecision.LINGER_IN_PANIC, decision);
    }

    @Test
    void decide_hostileStayingBeyondTheCalmDistanceForThePanicLinger_holds() {
        // Arrange
        ThreatSituation situation = ThreatSituation.builder()
                .previousVerdict(ThreatVerdict.PANIC)
                .danger(ThreatResponseRules.dangerContribution(beyondCalmDistance(), ORDINARY_THREAT_WEIGHT))
                .awareHostilePresent(true)
                .sinceAlarmed(ThreatResponseRules.PANIC_LINGER)
                .build();

        // Act -- counterexample: an unbounded linger, so a hostile loitering in awareness keeps the villager
        // panicking forever; or a non-strict comparison keeping it one assessment past the linger.
        ThreatDecision decision = ThreatResponseRules.decide(situation);

        // Assert
        assertEquals(ThreatDecision.HOLD, decision);
    }

    @Test
    void decide_hostileClosingInDuringTheLinger_alarmsAgain() {
        // Arrange
        ThreatSituation situation = ThreatSituation.builder()
                .previousVerdict(ThreatVerdict.PANIC)
                .danger(ThreatResponseRules.dangerContribution(VANILLA_ZOMBIE_PANIC_RANGE_BLOCKS, ORDINARY_THREAT_WEIGHT))
                .awareHostilePresent(true)
                .sinceAlarmed(JUST_UNDER_PANIC_LINGER)
                .build();

        // Act -- counterexample: the linger checked before the alarm, so a hostile closing in mid-linger never
        // restarts the clock and the villager calms down with the hostile at its heels.
        ThreatDecision decision = ThreatResponseRules.decide(situation);

        // Assert
        assertEquals(ThreatDecision.PANIC, decision);
    }

    @Test
    void decide_noHostileLeftInAwarenessDuringTheLinger_holds() {
        // Arrange -- the attacker died the moment after the villager was last alarmed.
        ThreatSituation situation = ThreatSituation.builder()
                .previousVerdict(ThreatVerdict.PANIC)
                .awareHostilePresent(false)
                .sinceAlarmed(ClockTicks.ZERO)
                .build();

        // Act -- counterexample: a linger keyed on time alone, so a villager keeps fleeing a hostile that is gone.
        ThreatDecision decision = ThreatResponseRules.decide(situation);

        // Assert
        assertEquals(ThreatDecision.HOLD, decision);
    }

    @Test
    void decide_recentlyAlarmedButNoLongerPanicking_doesNotLinger() {
        // Arrange -- the villager calmed when its hostile left awareness, and the hostile has just come back
        // beyond the alarm distance.
        ThreatSituation situation = ThreatSituation.builder()
                .previousVerdict(ThreatVerdict.HOLD)
                .danger(ThreatResponseRules.dangerContribution(beyondCalmDistance(), ORDINARY_THREAT_WEIGHT))
                .awareHostilePresent(true)
                .sinceAlarmed(ClockTicks.ZERO)
                .build();

        // Act -- counterexample: a linger keyed on a recent alarm rather than on panicking, so a calm villager
        // panics at a hostile too far away to alarm it.
        ThreatDecision decision = ThreatResponseRules.decide(situation);

        // Assert
        assertEquals(ThreatDecision.HOLD, decision);
    }

    /**
     * Midway between the calm distance and the falloff reach, where a hostile adds danger but too little to stay
     * alarmed.
     */
    private static double beyondCalmDistance() {
        double calmDistance = ThreatResponseRules.DANGER_FALLOFF_REACH_BLOCKS * (1.0 - ThreatResponseRules.DANGER_CALM_THRESHOLD);
        return (calmDistance + ThreatResponseRules.DANGER_FALLOFF_REACH_BLOCKS) / 2.0;
    }

    // --- Continuation and entry ---

    @Test
    void decide_noEngageableOption_cannotEnterOnAnotherOptionsNerve() {
        // Arrange -- nothing can engage a sighted hostile, while an option the villager is not fighting
        // with carries overwhelming nerve.
        ThreatSituation situation = ThreatSituation.builder()
                .previousVerdict(ThreatVerdict.HOLD)
                .danger(ThreatResponseRules.dangerContribution(CLOSE_DISTANCE_BLOCKS, ORDINARY_THREAT_WEIGHT))
                .awareHostilePresent(true)
                .continuingNerve(100.0)
                .build();

        // Act -- counterexample: entry authorized by whichever nerve is present, so an option with no
        // target to engage still enters COMBAT.
        ThreatDecision decision = ThreatResponseRules.decide(situation);

        // Assert -- still dangerous (the aware hostile), so it falls to PANIC rather than COMBAT.
        assertEquals(ThreatDecision.PANIC, decision);
    }

    @Test
    void decide_noOptionOnTheFirstAssessment_panicsWhenDangerous() {
        // Arrange -- counterexample: a first-ever assessment with no option defaulting to HOLD instead of
        // reacting to a hostile that is actually alarming.
        ThreatSituation situation = ThreatSituation.builder()
                .danger(ThreatResponseRules.dangerContribution(VANILLA_ZOMBIE_PANIC_RANGE_BLOCKS, ORDINARY_THREAT_WEIGHT))
                .awareHostilePresent(true)
                .build();

        // Act
        ThreatDecision decision = ThreatResponseRules.decide(situation);

        // Assert
        assertEquals(ThreatDecision.PANIC, decision);
    }

    @Test
    void decide_nerveExactlyAtEntryThreshold_doesNotEnterCombat() {
        // Arrange
        ThreatSituation situation = ThreatSituation.builder()
                .previousVerdict(ThreatVerdict.HOLD)
                .enteringNerve(ThreatResponseRules.NERVE_ENTRY_THRESHOLD)
                .build();

        // Act -- counterexample: a non-strict ">=" comparison admitting equality at the entry threshold.
        ThreatDecision decision = ThreatResponseRules.decide(situation);

        // Assert -- neither dangerous nor hit, so a refused entry falls all the way to HOLD.
        assertEquals(ThreatDecision.HOLD, decision);
    }

    @Test
    void decide_nerveExactlyAtContinueThreshold_continuesCombat() {
        // Arrange -- a live hit stands in for an aware hostile.
        ThreatSituation situation = ThreatSituation.builder()
                .previousVerdict(ThreatVerdict.COMBAT)
                .liveHitPresent(true)
                .continuingNerve(ThreatResponseRules.NERVE_CONTINUE_THRESHOLD)
                .sinceEngageableTarget(ClockTicks.ZERO)
                .build();

        // Act -- counterexample: a strict ">" comparison refusing equality at the continuation threshold,
        // ending a fight that should have held.
        ThreatDecision decision = ThreatResponseRules.decide(situation);

        // Assert
        assertEquals(ThreatDecision.CONTINUE_COMBAT, decision);
    }

    @Test
    void decide_targetLostForLessThanTheTargetlessLimit_continuesCombat() {
        // Arrange -- nothing can engage a sighted hostile any more (sight lost), but one hostile remains
        // in awareness.
        ThreatSituation situation = ThreatSituation.builder()
                .previousVerdict(ThreatVerdict.COMBAT)
                .awareHostilePresent(true)
                .continuingNerve(ThreatResponseRules.NERVE_CONTINUE_THRESHOLD + 1.0)
                .sinceEngageableTarget(JUST_UNDER_TARGETLESS_LIMIT)
                .build();

        // Act -- counterexample: continuation keyed on having a target right now, dropping a fighter
        // the instant its target steps behind a wall.
        ThreatDecision decision = ThreatResponseRules.decide(situation);

        // Assert
        assertEquals(ThreatDecision.CONTINUE_COMBAT, decision);
    }

    @Test
    void decide_awareHostileNeverEngageableForTheTargetlessLimit_endsCombat() {
        // Arrange -- a hostile the fighter can neither see nor reach, such as one in a cave below, stays in
        // awareness without adding enough danger to alarm.
        ThreatSituation situation = ThreatSituation.builder()
                .previousVerdict(ThreatVerdict.COMBAT)
                .awareHostilePresent(true)
                .continuingNerve(ThreatResponseRules.NERVE_CONTINUE_THRESHOLD + 1.0)
                .sinceEngageableTarget(ThreatResponseRules.TARGETLESS_COMBAT_LIMIT)
                .build();

        // Act -- counterexample: awareness alone holding the fight, so the villager stands in COMBAT
        // all night; or a non-strict comparison keeping it one assessment past the limit.
        ThreatDecision decision = ThreatResponseRules.decide(situation);

        // Assert
        assertEquals(ThreatDecision.HOLD, decision);
    }

    @Test
    void decide_hitByAnUnseenAttackerWithinTheTargetlessLimit_continuesCombat() {
        // Arrange -- no aware hostile at all (the attacker is outside awareness), but a live hit stands in
        // for it.
        ThreatSituation situation = ThreatSituation.builder()
                .previousVerdict(ThreatVerdict.COMBAT)
                .liveHitPresent(true)
                .continuingNerve(ThreatResponseRules.NERVE_CONTINUE_THRESHOLD + 1.0)
                .sinceEngageableTarget(JUST_UNDER_TARGETLESS_LIMIT)
                .build();

        // Act -- counterexample: continuation requiring an aware hostile specifically, dropping a
        // fighter that is still being hit by an attacker its awareness never picked up.
        ThreatDecision decision = ThreatResponseRules.decide(situation);

        // Assert
        assertEquals(ThreatDecision.CONTINUE_COMBAT, decision);
    }

    @Test
    void decide_hitByAnUnseenAttackerPastTheTargetlessLimit_panics() {
        // Arrange
        ThreatSituation situation = ThreatSituation.builder()
                .previousVerdict(ThreatVerdict.COMBAT)
                .liveHitPresent(true)
                .continuingNerve(ThreatResponseRules.NERVE_CONTINUE_THRESHOLD + 1.0)
                .sinceEngageableTarget(ThreatResponseRules.TARGETLESS_COMBAT_LIMIT)
                .build();

        // Act -- counterexample: a live hit exempting the fight from the targetless limit, so a fighter
        // shot by an attacker it cannot engage stands still instead of fleeing.
        ThreatDecision decision = ThreatResponseRules.decide(situation);

        // Assert
        assertEquals(ThreatDecision.PANIC, decision);
    }

    @Test
    void decide_liveHitWithoutAFight_panics() {
        // Arrange -- counterexample: PANIC gated on danger alone, so a live hit with nothing aware and
        // no usable option produces HOLD instead.
        ThreatSituation situation = ThreatSituation.builder()
                .liveHitPresent(true)
                .build();

        // Act
        ThreatDecision decision = ThreatResponseRules.decide(situation);

        // Assert
        assertEquals(ThreatDecision.PANIC, decision);
    }

    @Test
    void decide_nothingDangerousAndNoLiveHit_holds() {
        // Arrange -- counterexample: a default decision other than HOLD when nothing at all is wrong.
        ThreatSituation situation = ThreatSituation.builder().build();

        // Act
        ThreatDecision decision = ThreatResponseRules.decide(situation);

        // Assert
        assertEquals(ThreatDecision.HOLD, decision);
    }

    @Test
    void holds_whenCombatHasNoRemainingThreatEvidence() {
        // Arrange -- counterexample: sufficient nerve keeping a fight active after both awareness and
        // hit evidence disappear, even though no option has a target for a new engagement.
        ThreatSituation situation = ThreatSituation.builder()
                .previousVerdict(ThreatVerdict.COMBAT)
                .awareHostilePresent(false)
                .liveHitPresent(false)
                .continuingNerve(ThreatResponseRules.NERVE_CONTINUE_THRESHOLD + 1.0)
                .sinceEngageableTarget(ClockTicks.ZERO)
                .build();

        // Act
        ThreatDecision decision = ThreatResponseRules.decide(situation);

        // Assert
        assertEquals(ThreatDecision.HOLD, decision);
    }

    @Test
    void entersCombat_whenPreviousOptionCannotContinue() {
        // Arrange -- counterexample: losing the previous option immediately causing panic without
        // considering another option that can enter combat.
        ThreatSituation situation = ThreatSituation.builder()
                .previousVerdict(ThreatVerdict.COMBAT)
                .awareHostilePresent(true)
                .danger(ThreatResponseRules.DANGER_ALARM_THRESHOLD)
                .continuingNerve(null)
                .enteringNerve(ThreatResponseRules.NERVE_ENTRY_THRESHOLD + 1.0)
                .build();

        // Act
        ThreatDecision decision = ThreatResponseRules.decide(situation);

        // Assert
        assertEquals(ThreatDecision.ENTER_COMBAT, decision);
    }

    @Test
    void entersCombat_whenOnlyEnteringOptionHasEnoughNerve() {
        // Arrange -- counterexample: borrowing the entering option's higher nerve to continue the
        // current fight, or panicking before considering entry after continuation fails.
        ThreatSituation situation = ThreatSituation.builder()
                .previousVerdict(ThreatVerdict.COMBAT)
                .awareHostilePresent(true)
                .danger(ThreatResponseRules.DANGER_ALARM_THRESHOLD)
                .continuingNerve(Math.nextDown(ThreatResponseRules.NERVE_CONTINUE_THRESHOLD))
                .sinceEngageableTarget(ClockTicks.ZERO)
                .enteringNerve(ThreatResponseRules.NERVE_ENTRY_THRESHOLD + 1.0)
                .build();

        // Act
        ThreatDecision decision = ThreatResponseRules.decide(situation);

        // Assert
        assertEquals(ThreatDecision.ENTER_COMBAT, decision);
    }

    @Test
    void continuesCombat_whenEnteringOptionHasTooLittleNerve() {
        // Arrange -- counterexample: the weaker entering option's nerve replacing the current option's
        // score and ending a fight that has enough nerve to continue.
        ThreatSituation situation = ThreatSituation.builder()
                .previousVerdict(ThreatVerdict.COMBAT)
                .awareHostilePresent(true)
                .danger(ThreatResponseRules.DANGER_ALARM_THRESHOLD)
                .continuingNerve(ThreatResponseRules.NERVE_CONTINUE_THRESHOLD)
                .sinceEngageableTarget(ClockTicks.ZERO)
                .enteringNerve(Math.nextDown(ThreatResponseRules.NERVE_CONTINUE_THRESHOLD))
                .build();

        // Act
        ThreatDecision decision = ThreatResponseRules.decide(situation);

        // Assert
        assertEquals(ThreatDecision.CONTINUE_COMBAT, decision);
    }

    @Test
    void decide_healingReturnsAPanickedVillagerToCombat() {
        // Arrange -- a fighter still facing the same sighted, engageable hostile throughout; only its own
        // health changes. The option's nerve contribution is chosen so low health drops nerve out of COMBAT
        // (into PANIC, since the hostile still makes the area dangerous) and full health lifts it back over
        // the entry threshold.
        double optionNerveContribution = 0.8;
        double danger = ThreatResponseRules.dangerContribution(CLOSE_DISTANCE_BLOCKS, ORDINARY_THREAT_WEIGHT);
        double injuredNerve = ThreatResponseRules.nerve(AVERAGE_WILL, 0.0, optionNerveContribution, ORDINARY_THREAT_WEIGHT);
        ThreatSituation injured = ThreatSituation.builder()
                .previousVerdict(ThreatVerdict.COMBAT)
                .danger(danger)
                .awareHostilePresent(true)
                .continuingNerve(injuredNerve)
                .sinceEngageableTarget(ClockTicks.ZERO)
                .enteringNerve(injuredNerve)
                .build();
        assertEquals(ThreatDecision.PANIC, ThreatResponseRules.decide(injured), "test setup: low health must first drop nerve out of combat");

        // A PANIC verdict keeps no selection, so the healed villager has no continuing nerve.
        ThreatSituation healed = ThreatSituation.builder()
                .previousVerdict(ThreatVerdict.PANIC)
                .danger(danger)
                .awareHostilePresent(true)
                .enteringNerve(ThreatResponseRules.nerve(AVERAGE_WILL, FULL_HEALTH, optionNerveContribution, ORDINARY_THREAT_WEIGHT))
                .build();

        // Act -- counterexample: PANIC holding a villager that has recovered the nerve to fight, so a
        // healed fighter facing the same engageable hostile stays routed.
        ThreatDecision decision = ThreatResponseRules.decide(healed);

        // Assert
        assertEquals(ThreatDecision.ENTER_COMBAT, decision);
    }

    @Test
    void decide_aContinuingFightIsPreferredOverAnEntryThatWouldAlsoSucceed() {
        // Arrange -- the previous option's nerve is just enough to keep fighting, while the first
        // engageable option would clear the entry threshold comfortably.
        ThreatSituation situation = ThreatSituation.builder()
                .previousVerdict(ThreatVerdict.COMBAT)
                .liveHitPresent(true)
                .continuingNerve(ThreatResponseRules.NERVE_CONTINUE_THRESHOLD)
                .sinceEngageableTarget(ClockTicks.ZERO)
                .enteringNerve(ThreatResponseRules.NERVE_ENTRY_THRESHOLD + 1.0)
                .build();

        // Act -- counterexample: entry checked before continuation, so an available alternative displaces
        // a fight that should have continued.
        ThreatDecision decision = ThreatResponseRules.decide(situation);

        // Assert
        assertEquals(ThreatDecision.CONTINUE_COMBAT, decision);
    }

    @Test
    void decide_aRoutedVillagerReEntersRatherThanContinues() {
        // Arrange -- the option held before routing still carries overwhelming nerve, and an engageable
        // option could enter. The previous verdict is PANIC, as it would be the tick after routing.
        ThreatSituation situation = ThreatSituation.builder()
                .previousVerdict(ThreatVerdict.PANIC)
                .awareHostilePresent(true)
                .continuingNerve(100.0)
                .sinceEngageableTarget(ClockTicks.ZERO)
                .enteringNerve(ThreatResponseRules.NERVE_ENTRY_THRESHOLD + 0.1)
                .build();

        // Act -- counterexample: continuation firing from PANIC, resuming the option held before routing
        // instead of re-entering through the entry rule.
        ThreatDecision decision = ThreatResponseRules.decide(situation);

        // Assert
        assertEquals(ThreatDecision.ENTER_COMBAT, decision);
    }

}
