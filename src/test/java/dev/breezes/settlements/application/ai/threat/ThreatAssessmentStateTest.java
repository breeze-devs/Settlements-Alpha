package dev.breezes.settlements.application.ai.threat;

import dev.breezes.settlements.domain.ai.perception.PerceivedEntities;
import dev.breezes.settlements.domain.ai.threat.QualifyingHit;
import dev.breezes.settlements.domain.ai.threat.ThreatDecision;
import dev.breezes.settlements.domain.ai.threat.ThreatVerdict;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreatAssessmentStateTest {

    private static final UUID ATTACKER_A = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ATTACKER_B = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID HOSTILE = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final ThreatAssessmentResult HOLD_RESULT = new ThreatAssessmentResult(ThreatVerdict.HOLD, null);
    private static final ThreatAssessmentResult PANIC_RESULT = new ThreatAssessmentResult(ThreatVerdict.PANIC, null);

    @Test
    void recordHit_withNoAttacker_leavesTheStateUntouched() {
        // Arrange -- counterexample: environmental damage (fall, fire) marking the villager as
        // threatened with no attacker to flee from or fight.
        ThreatAssessmentState state = new ThreatAssessmentState();

        // Act
        Optional<QualifyingHit> result = state.recordHit(null, 10.0f, 0L);

        // Assert
        assertTrue(result.isEmpty());
        assertNull(state.getLatestHit());
        assertFalse(state.isAssessmentDirty());
    }

    @Test
    void recordHit_theSmallestPossibleHealthLoss_isRecorded() {
        // Arrange -- counterexample: a leftover minimum-damage threshold silently ignoring a small hit
        // from a real attacker.
        ThreatAssessmentState state = new ThreatAssessmentState();

        // Act
        Optional<QualifyingHit> result = state.recordHit(ATTACKER_A, Float.MIN_VALUE, 0L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(ATTACKER_A, state.getLatestHit().attackerId());
    }

    @Test
    void recordHit_aHitCostingNoHealthAfterAQualifyingOne_leavesAmountAndTimeUntouched() {
        // Arrange -- counterexample: a hit that cost no health landing after a real hit either erasing
        // the remembered attack or refreshing its recorded time, either of which would distort recovery.
        ThreatAssessmentState state = new ThreatAssessmentState();
        state.recordHit(ATTACKER_A, 5.0f, 100L);

        // Act
        Optional<QualifyingHit> ignored = state.recordHit(ATTACKER_B, 0.0f, 999L);

        // Assert
        assertTrue(ignored.isEmpty());
        QualifyingHit observation = state.getLatestHit();
        assertEquals(5.0f, observation.healthLost());
        assertEquals(100L, observation.gameTime());
        assertEquals(ATTACKER_A, observation.attackerId());
    }

    @Test
    void recordHit_twoQualifyingHits_areNotSummed() {
        // Arrange -- counterexample: accumulating health lost across hits instead of tracking the
        // latest one, which would make repeated small hits look like one large one.
        ThreatAssessmentState state = new ThreatAssessmentState();
        state.recordHit(ATTACKER_A, 5.0f, 100L);

        // Act
        state.recordHit(ATTACKER_A, 3.0f, 200L);

        // Assert
        assertEquals(3.0f, state.getLatestHit().healthLost());
    }

    @Test
    void recordHit_aSecondQualifyingHit_replacesTheAttackerAndTime() {
        // Arrange -- counterexample: keeping the first attacker on record after a second, different
        // attacker lands a later qualifying hit.
        ThreatAssessmentState state = new ThreatAssessmentState();
        state.recordHit(ATTACKER_A, 5.0f, 100L);

        // Act
        state.recordHit(ATTACKER_B, 5.0f, 200L);

        // Assert
        QualifyingHit observation = state.getLatestHit();
        assertEquals(ATTACKER_B, observation.attackerId());
        assertEquals(200L, observation.gameTime());
    }

    @Test
    void recordHit_aQualifyingHit_marksTheAssessmentDirty() {
        // Arrange
        ThreatAssessmentState state = new ThreatAssessmentState();

        // Act
        state.recordHit(ATTACKER_A, 5.0f, 0L);

        // Assert -- counterexample: a qualifying hit landing with nothing signaling the next
        // assessment to actually react to it.
        assertTrue(state.isAssessmentDirty());
    }

    @Test
    void isDue_whenTheAssessmentIsDirty_isDueImmediately() {
        // Arrange -- counterexample: a qualifying hit waiting for the cadence like anything else,
        // delaying a villager's reaction to a real attacker by up to a full interval.
        ThreatAssessmentState state = new ThreatAssessmentState();
        state.recordHit(ATTACKER_A, 5.0f, 0L);

        // Act
        boolean due = state.isDue(PerceivedEntities.empty());

        // Assert
        assertTrue(due);
    }

    @Test
    void isDue_becomesDueOnceTheCadenceIntervalElapses_withNoDirtyHitOrPerceptionChange() {
        // Arrange -- counterexample: a due-decision that never becomes true on its own, starving every
        // villager's periodic reassessment down to only dirty hits and perception changes.
        ThreatAssessmentState state = new ThreatAssessmentState();
        PerceivedEntities noHostiles = PerceivedEntities.empty();
        int interval = ThreatAssessmentState.ASSESSMENT_INTERVAL.getTicksAsInt();

        // Act -- bounded well past one interval so a starved cadence fails the assertion, not the build.
        int firstDueTick = Integer.MAX_VALUE;
        for (int tick = 1; tick <= interval * 2; tick++) {
            if (state.isDue(noHostiles)) {
                firstDueTick = tick;
                break;
            }
        }

        // Assert -- the staggered phase lands somewhere within the first interval.
        assertTrue(firstDueTick <= interval, "cadence must fire within its first interval");
    }

    @Test
    void isDue_distinctButContentEqualPerceivedEntities_doesNotTriggerAFreshPerceptionCheck() {
        // Arrange -- counterexample: comparing NEARBY_HOSTILES by reference instead of content, so a
        // freshly built but unchanged (empty) perception spuriously forces reassessment every tick.
        ThreatAssessmentState state = new ThreatAssessmentState();
        PerceivedEntities firstEmpty = new PerceivedEntities(List.of());
        PerceivedEntities secondDistinctButEqualEmpty = new PerceivedEntities(List.of());
        assertNotSame(firstEmpty, secondDistinctButEqualEmpty, "test setup: instances must be distinct to exercise equality, not identity");

        // A dirty-triggered assessment restarts the cadence, so the tick under test cannot land on its
        // random first firing.
        state.recordHit(ATTACKER_A, 5.0f, 0L);
        state.isDue(firstEmpty);
        state.recordAssessment(ThreatDecision.HOLD, HOLD_RESULT, firstEmpty, 0L);

        // Act -- the tick right after the restart, with a distinct-but-equal instance.
        boolean due = state.isDue(secondDistinctButEqualEmpty);

        // Assert
        assertFalse(due);
    }

    @Test
    void isDue_aDirtyTriggeredAssessment_restartsTheFullCadenceInterval() {
        // Arrange -- counterexample: the cadence continuing to count down through a dirty-triggered
        // assessment instead of restarting, so a villager with a hostile nearby assesses both on the
        // dirty trigger and again on the cadence's original, un-restarted phase shortly after -- up to
        // twice the intended rate.
        ThreatAssessmentState state = new ThreatAssessmentState();
        PerceivedEntities noHostiles = PerceivedEntities.empty();
        int interval = ThreatAssessmentState.ASSESSMENT_INTERVAL.getTicksAsInt();

        state.recordHit(ATTACKER_A, 5.0f, 0L);
        assertTrue(state.isDue(noHostiles)); // tick 1: dirty-triggered
        state.recordAssessment(ThreatDecision.HOLD, HOLD_RESULT, noHostiles, 0L);

        // Act & Assert -- the next interval-1 ticks must not be due; the cadence must not fire on
        // whatever tick its original, un-restarted phase would have completed at.
        for (int tick = 2; tick < 1 + interval; tick++) {
            assertFalse(state.isDue(noHostiles), "tick " + tick + " must not be due yet");
        }
        assertTrue(state.isDue(noHostiles), "cadence must fire a full interval after the dirty-triggered assessment");
    }

    @Test
    void recordAssessment_clearsTheDirtyFlag() {
        // Arrange -- counterexample: a dirty flag surviving its own assessment, forcing every following
        // tick to run a full assessment regardless of the cadence or perception.
        ThreatAssessmentState state = new ThreatAssessmentState();
        state.recordHit(ATTACKER_A, 5.0f, 0L);

        // Act
        state.recordAssessment(ThreatDecision.HOLD, HOLD_RESULT, PerceivedEntities.empty(), 0L);

        // Assert
        assertFalse(state.isAssessmentDirty());
    }

    @Test
    void recordAssessment_aContinuationWithNoTarget_keepsTheLastTargetedTime() {
        // Arrange -- counterexample: every COMBAT result counting as engagement, so a fight against a
        // hostile it can never engage never runs out its targetless limit; or a targeted result never
        // recorded, so every fight ends at its first moment without a target.
        ThreatAssessmentState state = new ThreatAssessmentState();
        CombatOption option = FakeCombatOptions.withOrder(1);
        long targetedTime = 100L;
        state.recordAssessment(ThreatDecision.ENTER_COMBAT, new ThreatAssessmentResult(ThreatVerdict.COMBAT, new CombatSelection(option, HOSTILE)),
                PerceivedEntities.empty(), targetedTime);

        // Act
        state.recordAssessment(ThreatDecision.CONTINUE_COMBAT, new ThreatAssessmentResult(ThreatVerdict.COMBAT, new CombatSelection(option, null)),
                PerceivedEntities.empty(), targetedTime + 50L);

        // Assert
        assertEquals(targetedTime, state.getLastEngagedGameTime());
    }

    @Test
    void recordAssessment_aLingeringPanic_keepsTheLastAlarmedTime() {
        // Arrange -- counterexample: every PANIC result counting as alarm, so a lingering panic restarts its own
        // clock and a hostile loitering in awareness holds the villager in PANIC forever; or an alarming result
        // never recorded, so every panic calms the moment it stops being alarming.
        ThreatAssessmentState state = new ThreatAssessmentState();
        long alarmedTime = 100L;
        state.recordAssessment(ThreatDecision.PANIC, PANIC_RESULT, PerceivedEntities.empty(), alarmedTime);

        // Act
        state.recordAssessment(ThreatDecision.LINGER_IN_PANIC, PANIC_RESULT, PerceivedEntities.empty(), alarmedTime + 50L);

        // Assert
        assertEquals(alarmedTime, state.getLastAlarmedGameTime());
    }

}
